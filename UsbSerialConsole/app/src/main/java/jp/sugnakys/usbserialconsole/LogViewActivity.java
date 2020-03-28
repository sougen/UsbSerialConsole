package jp.sugnakys.usbserialconsole;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import jp.sugnakys.usbserialconsole.util.Constants;

public class LogViewActivity extends BaseAppCompatActivity {

    private static final String TAG = "LogViewActivity";

    private File logFile;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.log_view_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
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
    }

    public void onResume() {
        super.onResume();

        logFile = (File) getIntent().getExtras().get(Constants.EXTRA_LOG_FILE);
        if (logFile != null) {
            toolbar.setTitle(logFile.getName());
            setLogText(logFile);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_log_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_send_to:
                intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(logFile), "text/plain");
                startActivity(intent);
                break;
            default:
                Log.e(TAG, "Unknown id");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setLogText(File file) {
        TextView textView = (TextView) findViewById(R.id.logView);
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            byte[] readBytes = new byte[fileInputStream.available()];
            if (fileInputStream.read(readBytes) != -1) {
                String readString = new String(readBytes, Charset.forName(Constants.CHARSET));
                textView.setText(readString);
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}