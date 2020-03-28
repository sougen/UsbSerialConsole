package jp.sugnakys.usbserialconsole;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.sugnakys.usbserialconsole.settings.SettingsActivity;
import jp.sugnakys.usbserialconsole.util.Constants;
import jp.sugnakys.usbserialconsole.util.Log;
import jp.sugnakys.usbserialconsole.util.Util;

public class MainActivity extends BaseAppCompatActivity
        implements View.OnClickListener, TextWatcher {

    private static final String TAG = "MainActivity";
    private static final String RECEIVED_TEXT_VIEW_STR = "RECEIVED_TEXT_VIEW_STR";

    private UsbService usbService;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    private LinearLayout mainLayout;
    private Button sendBtn;
    private EditText sendMsgView;
    private LinearLayout sendViewLayout;
    private TextView receivedMsgView;
    private ScrollView scrollView;

    private Menu mOptionMenu;

    private MyHandler mHandler;

    private String timestampFormat;
    private String lineFeedCode;
    private String tmpReceivedData = "";

    private boolean showTimeStamp;
    private boolean isUSBReady = false;
    private boolean isConnect = false;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED:
                    Toast.makeText(context,
                            getString(R.string.usb_permission_granted),
                            Toast.LENGTH_SHORT).show();
                    isUSBReady = true;
                    updateOptionsMenu();
                    requestConnection();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED:
                    Toast.makeText(context,
                            getString(R.string.usb_permission_not_granted),
                            Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB:
                    Toast.makeText(context,
                            getString(R.string.no_usb),
                            Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED:
                    Toast.makeText(context,
                            getString(R.string.usb_disconnected),
                            Toast.LENGTH_SHORT).show();
                    isUSBReady = false;
                    stopConnection();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED:
                    Toast.makeText(context, getString(R.string.usb_not_supported),
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Log.e(TAG, "Unknown action");
                    break;
            }
        }
    };

    private void requestConnection() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setMessage(getString(R.string.confirm_connect));
        alertDialog.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startConnection();
            }
        });
        alertDialog.setNegativeButton(getString(android.R.string.cancel), null);
        alertDialog.create().show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_NoActionBar);

        mHandler = new MyHandler(this);

        setContentView(R.layout.activity_main);

        mainLayout = (LinearLayout) findViewById(R.id.mainLayout);
        receivedMsgView = (TextView) findViewById(R.id.receivedMsgView);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        sendBtn = (Button) findViewById(R.id.sendBtn);
        sendMsgView = (EditText) findViewById(R.id.sendMsgView);
        sendViewLayout = (LinearLayout) findViewById(R.id.sendViewLayout);

        sendBtn.setOnClickListener(this);
        sendMsgView.addTextChangedListener(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setDefaultColor() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();

        if (!pref.contains(getString(R.string.color_console_background_key))) {
            int defaultBackgroundColor = Color.TRANSPARENT;
            Drawable background = mainLayout.getBackground();
            if (background instanceof ColorDrawable) {
                defaultBackgroundColor = ((ColorDrawable) background).getColor();
            }
            editor.putInt(getString(R.string.color_console_background_key), defaultBackgroundColor);
            editor.apply();
            Log.d(TAG, "Default background color: " + String.format("#%08X", defaultBackgroundColor));
        }

        if (!pref.contains(getString(R.string.color_console_text_key))) {
            int defaultTextColor = receivedMsgView.getTextColors().getDefaultColor();
            editor.putInt(getString(R.string.color_console_text_key), defaultTextColor);
            editor.apply();
            Log.d(TAG, "Default text color: " + String.format("#%08X", defaultTextColor));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(RECEIVED_TEXT_VIEW_STR, receivedMsgView.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        receivedMsgView.setText(savedInstanceState.getString(RECEIVED_TEXT_VIEW_STR));
    }

    @Override
    public void onResume() {
        super.onResume();

        setDefaultColor();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        showTimeStamp = pref.getBoolean(
                getResources().getString(R.string.timestamp_visible_key), true);
        timestampFormat = pref.getString(getString(R.string.timestamp_format_key),
                getString(R.string.timestamp_format_default));
        lineFeedCode = Util.getLineFeedCd(
                pref.getString(getString(R.string.line_feed_code_send_key),
                        getString(R.string.line_feed_code_cr_lf_value)),
                this);
        if (pref.getBoolean(getString(R.string.send_form_visible_key), true)) {
            sendViewLayout.setVisibility(View.VISIBLE);
        } else {
            sendViewLayout.setVisibility(View.GONE);
        }

        if (pref.getBoolean(getString(R.string.sleep_mode_key), false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        int backgroundColor = pref.getInt(getString(R.string.color_console_background_key), Color.WHITE);
        Log.d(TAG, "Background color: " + String.format("#%08X", backgroundColor));
        mainLayout.setBackgroundColor(backgroundColor);

        int textColor = pref.getInt(getString(R.string.color_console_text_key), Color.BLACK);
        Log.d(TAG, "Text color: " + String.format("#%08X", textColor));
        receivedMsgView.setTextColor(textColor);
        sendMsgView.setTextColor(textColor);

        setFilters();
        startService(UsbService.class, usbConnection);
        updateOptionsMenu();
    }

    @Override
    public void onDestroy() {
        if (isConnect) {
            stopConnection();
        }
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);

        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
            alertDialog.setMessage(getString(R.string.confirm_finish_text));
            alertDialog.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            alertDialog.setNegativeButton(getString(android.R.string.cancel), null);
            alertDialog.create().show();
            return true;
        }
        return false;
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        sendBtn.setEnabled(!(s.length() == 0));
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionMenu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void updateOptionsMenu() {
        if (mOptionMenu != null) {
            onPrepareOptionsMenu(mOptionMenu);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_connect);
        item.setEnabled(isUSBReady);
        if (isConnect) {
            item.setTitle(getString(R.string.action_disconnect));
        } else {
            item.setTitle(getString(R.string.action_connect));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_connect:
                android.util.Log.d(TAG, "Connect clicked");
                if (isConnect) {
                    stopConnection();
                } else {
                    startConnection();
                }
                break;
            case R.id.action_clear_log:
                Log.d(TAG, "Clear log clicked");
                receivedMsgView.setText("");
                break;
            case R.id.action_save_log:
                Log.d(TAG, "Save log clicked");
                writeToFile(receivedMsgView.getText().toString());
                break;
            case R.id.action_settings:
                Log.d(TAG, "Settings clicked");
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.action_log_list:
                Log.d(TAG, "Log list clicked");
                intent = new Intent(this, LogListViewActivity.class);
                startActivity(intent);
                break;
            default:
                Log.e(TAG, "Unknown id");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void writeToFile(String data) {

        FileOutputStream fos = null;
        try {
            String fileName = Util.getCurrentDateForFile() + Constants.LOG_EXT;
            File dirName = Util.getLogDir(getApplicationContext());

            File outputFile = new File(dirName, fileName);

            fos = new FileOutputStream(outputFile);
            fos.write(data.getBytes(Constants.CHARSET));

            Log.d(TAG, "Save: " + outputFile.getCanonicalPath());

            String toastStr;
            if (Util.isInternalDir(getApplicationContext(), dirName)) {
                toastStr = getString(R.string.action_save_log) + " : " + fileName;
            } else {
                toastStr = getString(R.string.action_save_log) + " : " + outputFile.getCanonicalPath();
            }
            Toast.makeText(this, toastStr, Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    private void sendMessage(String msg) {
        Pattern pattern = Pattern.compile("\n$");
        Matcher matcher = pattern.matcher(msg);
        String strResult = matcher.replaceAll("") + lineFeedCode;
        try {
            usbService.write(strResult.getBytes(Constants.CHARSET));
            Log.d(TAG, "SendMessage: " + msg);
            addReceivedData(msg);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.toString());
        }
    }

    private void startConnection() {
        usbService.setHandler(mHandler);
        isConnect = true;
        Toast.makeText(getApplicationContext(),
                getString(R.string.start_connection), Toast.LENGTH_SHORT).show();
        updateOptionsMenu();
    }

    private void stopConnection() {
        usbService.setHandler(null);
        isConnect = false;
        Toast.makeText(getApplicationContext(),
                getString(R.string.stop_connection), Toast.LENGTH_SHORT).show();
        updateOptionsMenu();
    }

    private void addReceivedData(String data) {
        if (showTimeStamp) {
            addReceivedDataWithTime(data);
        } else {
            addTextView(data);
        }
    }

    private void addTextView(String data) {
        receivedMsgView.append(data);
        scrollView.scrollTo(0, receivedMsgView.getBottom());
    }

    private void addReceivedDataWithTime(String data) {
        String timeStamp = "[" + Util.getCurrentTime(timestampFormat) + "] ";

        tmpReceivedData += data;
        String separateStr = getLineSeparater(tmpReceivedData);
        if (!separateStr.isEmpty()) {
            String[] strArray = tmpReceivedData.split(separateStr);
            tmpReceivedData = "";
            for (int i = 0; i < strArray.length; i++) {
                if (strArray.length != 1
                        && i == (strArray.length - 1)
                        && !strArray[i].isEmpty()) {
                    tmpReceivedData = strArray[i];
                } else {
                    addTextView(timeStamp + strArray[i] + System.lineSeparator());
                }
            }
        }
    }

    private String getLineSeparater(String str) {
        if (str.contains(Constants.CR_LF)) {
            return Constants.CR_LF;
        } else if (str.contains(Constants.LF)) {
            return Constants.LF;
        } else if (str.contains(Constants.CR)) {
            return Constants.CR;
        } else {
            return "";
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sendBtn:
                android.util.Log.d(TAG, "Send button clicked");
                String message = sendMsgView.getText().toString();
                if (!message.isEmpty()) {
                    message += System.lineSeparator();
                    sendMessage(message);
                    sendMsgView.setText("");
                }
            default:
                android.util.Log.e(TAG, "Unknown view");
                break;
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    if (data != null) {
                        mActivity.get().addReceivedData(data);
                    }
                    break;
                case UsbService.CTS_CHANGE:
                    Log.d(TAG, "CTS_CHANGE");
                    Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Log.d(TAG, "DSR_CHANGE");
                    Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show();
                    break;
                default:
                    Log.e(TAG, "Unknown message");
                    break;
            }
        }
    }
}
