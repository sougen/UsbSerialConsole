package jp.co.sugnakys.usbserialconsole;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private UsbService usbService;
    private MyHandler mHandler;

    private TextView receivedMsgView;
    private ScrollView scrollView;
    private Button connectBtn;

    private boolean showTimeStamp = true;
    private String timestampFormat;

    private boolean hasLineSeparator = true;
    private String tmpReceivedData = "";

    private static final String RECEIVED_TEXT_VIEW_STR = "RECEIVED_TEXT_VIEW_STR";

    private boolean isConnect = false;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED:
                    Toast.makeText(context, getString(R.string.usb_permission_granted), Toast.LENGTH_SHORT).show();
                    connectBtn.setEnabled(true);
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED:
                    Toast.makeText(context, getString(R.string.usb_permission_not_granted), Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB:
                    Toast.makeText(context, getString(R.string.no_usb), Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED:
                    Toast.makeText(context, getString(R.string.usb_disconnected), Toast.LENGTH_SHORT).show();
                    if (toggleShowLog()) {
                        connectBtn.setText(getResources().getString(R.string.disconnect));
                    } else {
                        connectBtn.setText(getResources().getString(R.string.connect));
                    }
                    connectBtn.setEnabled(false);
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED:
                    Toast.makeText(context, getString(R.string.usb_not_supported), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Log.e(TAG, "Unknown action");
                    break;
            }
        }
    };

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new MyHandler(this);

        setContentView(R.layout.activity_main);
        connectBtn = (Button) findViewById(R.id.connectBtn);
        receivedMsgView = (TextView) findViewById(R.id.receivedMsgView);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        showTimeStamp = pref.getBoolean(getResources().getString(R.string.timestamp_key), true);
        timestampFormat = pref.getString(getString(R.string.timestamp_format_key), getString(R.string.timestamp_format_default));

        setFilters();
        startService(UsbService.class, usbConnection);
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.action_log_list:
                intent = new Intent(this, LogListViewActivity.class);
                startActivity(intent);
                break;
            default:
                Log.e(TAG, "Unknown id");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void writeToFile(String data) {
        String fileName = Util.getCurrentDateForFile() + Constants.LOG_EXT;
        File dirName = Util.getLogDir(getApplicationContext());

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(dirName, fileName));
            fos.write(data.getBytes(Constants.CHARSET));
            Toast.makeText(this, "Save: " + fileName, Toast.LENGTH_SHORT).show();
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

    public void sendMessage(String msg) {
        try {
            usbService.write(msg.getBytes(Constants.CHARSET));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.toString());
        }
    }

    public boolean toggleShowLog() {
        if (isConnect) {
            usbService.setHandler(null);
            isConnect = false;
        } else {
            usbService.setHandler(mHandler);
            isConnect = true;
        }
        return isConnect;
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
                    mActivity.get().addReceivedData(data);
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show();
                    break;
                default:
                    Log.e(TAG, "Unknown message");
                    break;
            }
        }
    }

    public void addReceivedData(String data) {
        if (showTimeStamp && hasLineSeparator) {
            tmpReceivedData = "[" + Util.getCurrentTime(timestampFormat) + "] ";
            hasLineSeparator = false;
        }

        tmpReceivedData += data;

        if (data.contains(System.getProperty("line.separator"))) {
            receivedMsgView.append(tmpReceivedData);
            tmpReceivedData = "";
            scrollView.scrollTo(0, receivedMsgView.getBottom());
            hasLineSeparator = true;
        }
    }
}