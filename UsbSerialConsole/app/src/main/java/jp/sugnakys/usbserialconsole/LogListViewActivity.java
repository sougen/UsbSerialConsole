package jp.sugnakys.usbserialconsole;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import jp.sugnakys.usbserialconsole.util.Constants;
import jp.sugnakys.usbserialconsole.util.Util;

public class LogListViewActivity extends BaseAppCompatActivity
        implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final String TAG = "LogListViewActivity";

    private ListView listView;
    private SectionAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_list_view_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.action_log_list));
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        }

        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateList();
    }

    private boolean deleteLogFile(File file) {
        if (file == null) {
            return false;
        }
        Log.d(TAG, "Delete file path: " + file.getName());
        return file.delete();
    }

    private void updateList() {
        File internalDir = Util.getInternalDir(getApplicationContext());
        File externalDir = Util.getExternalDir(getApplicationContext());

        File[] internalFiles;
        File[] externalFiles = null;

        // internalFiles is always true
        internalFiles = internalDir.listFiles(new FileFilter());
        if (internalFiles == null || internalFiles.length == 0) {
            internalFiles = null;
        }

        if (externalDir != null) {
            externalFiles = externalDir.listFiles(new FileFilter());
            if (externalFiles == null || externalFiles.length == 0) {
                externalFiles = null;
            }
        }

        if (internalFiles == null && externalFiles == null) {
            Log.w(TAG, "File not found");
            listView.setEmptyView(findViewById(android.R.id.empty));
            return;
        }

        List<BindData> data = new ArrayList<>();
        if (internalFiles != null) {
            data.add(new BindData(null, getString(R.string.log_internal_storage), true));
            for (File file: internalFiles) {
                data.add(new BindData(file, file.getName(), false));
            }
        }

        if (externalFiles != null) {
            data.add(new BindData(null, getString(R.string.log_external_storage), true));
            for (File file: externalFiles) {
                data.add(new BindData(file, file.getName(), false));
            }
        }

        adapter = new SectionAdapter(this, data);
        listView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        ListView listView = (ListView) adapterView;
        BindData selectedItem = (BindData) listView.getItemAtPosition(position);

        if (!selectedItem.isSection) {
            File targetFile = selectedItem.file;

            if (targetFile != null) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(targetFile), "text/plain");
                startActivity(intent);
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        ListView listView = (ListView) adapterView;
        final BindData selectedItem = (BindData) listView.getItemAtPosition(position);

        if (!selectedItem.isSection) {
            new AlertDialog.Builder(LogListViewActivity.this)
                    .setTitle(getResources().getString(R.string.delete_log_file_title))
                    .setMessage(getResources().getString(R.string.delete_log_file_text) + "\n"
                            + getResources().getString(R.string.file_name) + ": " + selectedItem.text)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    File targetFile = selectedItem.file;
                                    if (deleteLogFile(targetFile)) {
                                        adapter.clear();
                                        updateList();
                                    }
                                }
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
        return true;
    }

    private class FileFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return name.matches(Constants.LOG_EXT_MATCH);
        }
    }

    private class SectionAdapter extends ArrayAdapter<BindData> {
        private final LayoutInflater inflater;
        private final int layoutId;

        private SectionAdapter(Context context, List<BindData> objects) {
            super(context, 0, objects);
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.layoutId = android.R.layout.simple_list_item_1;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = inflater.inflate(layoutId, parent, false);
                holder = new ViewHolder();
                holder.textView = (TextView) convertView.findViewById(android.R.id.text1);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            BindData data = getItem(position);
            if (data != null) {
                if (data.isSection) {
                    convertView.setBackgroundColor(getColor(R.color.colorPrimaryDark));
                } else {
                    convertView.setBackgroundColor(Color.TRANSPARENT);
                }
                holder.textView.setText(data.text);
            }

            return convertView;
        }
    }

    private class BindData {
        final File file;
        final String text;
        final boolean isSection;

        private BindData(File file, String text, boolean isSection) {
            this.file = file;
            this.text = text;
            this.isSection = isSection;
        }
    }

    private static class ViewHolder {
        TextView textView;
    }
}
