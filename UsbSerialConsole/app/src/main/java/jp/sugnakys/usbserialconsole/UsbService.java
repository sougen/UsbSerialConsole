package jp.sugnakys.usbserialconsole;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jp.sugnakys.usbserialconsole.util.Constants;

public class UsbService extends Service {
    private static final String TAG = "UsbService";
    private static final boolean DBG = true;

    public static final String ACTION_USB_NOT_SUPPORTED = "jp.sugnakys.usbserialconsole.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "jp.sugnakys.usbserialconsole.NO_USB";
    public static final String ACTION_USB_PERMISSION_GRANTED = "jp.sugnakys.usbserialconsole.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "jp.sugnakys.usbserialconsole.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "jp.sugnakys.usbserialconsole.USB_DISCONNECTED";
    public static final String ACTION_SERIAL_CONFIG_CHANGED = "jp.sugnakys.usbserialconsole.SERIAL_CONFIG_CHANGED";

    private static final String ACTION_USB_READY = "jp.sugnakys.usbserialconsole.USB_READY";
    private static final String ACTION_CDC_DRIVER_NOT_WORKING = "jp.sugnakys.usbserialconsole.ACTION_CDC_DRIVER_NOT_WORKING";
    private static final String ACTION_USB_DEVICE_NOT_WORKING = "jp.sugnakys.usbserialconsole.ACTION_USB_DEVICE_NOT_WORKING";
    private static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    private static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    private static final String ACTION_USB_PERMISSION = "jp.sugnakys.usbserialconsole.USB_PERMISSION";

    public static final int MESSAGE_FROM_SERIAL_PORT = 0;
    public static final int CTS_CHANGE = 1;
    public static final int DSR_CHANGE = 2;

    static boolean SERVICE_CONNECTED = false;

    private final IBinder binder = new UsbBinder();

    private Context context;
    private Handler mHandler;

    private final UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg) {
            try {
                String data = new String(arg, Constants.CHARSET);
                if (mHandler != null) {
                    mHandler.obtainMessage(MESSAGE_FROM_SERIAL_PORT, data).sendToTarget();
                }
            } catch (UnsupportedEncodingException e) {
                Log.d(TAG, e.toString());
            }
        }
    };

    private final UsbSerialInterface.UsbCTSCallback ctsCallback = new UsbSerialInterface.UsbCTSCallback() {
        @Override
        public void onCTSChanged(boolean state) {
            if (mHandler != null) {
                mHandler.obtainMessage(CTS_CHANGE).sendToTarget();
            }
        }
    };

    private final UsbSerialInterface.UsbDSRCallback dsrCallback = new UsbSerialInterface.UsbDSRCallback() {
        @Override
        public void onDSRChanged(boolean state) {
            if (mHandler != null) {
                mHandler.obtainMessage(DSR_CHANGE).sendToTarget();
            }
        }
    };

    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;
    private boolean serialPortConnected;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case ACTION_USB_PERMISSION:
                    boolean granted =
                            Objects.requireNonNull(intent.getExtras()).getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    if (granted) {
                        Intent in = new Intent(ACTION_USB_PERMISSION_GRANTED);
                        context.sendBroadcast(in);
                        connection = usbManager.openDevice(device);
                        new ConnectionThread().start();
                    } else {
                        Intent in = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                        context.sendBroadcast(in);
                    }
                    break;
                case ACTION_USB_ATTACHED:
                    if (!serialPortConnected) {
                        findSerialPortDevice();
                    }
                    break;
                case ACTION_USB_DETACHED:
                    Intent in = new Intent(ACTION_USB_DISCONNECTED);
                    context.sendBroadcast(in);
                    if (serialPortConnected) {
                        serialPort.close();
                    }
                    serialPortConnected = false;
                    break;
                case ACTION_SERIAL_CONFIG_CHANGED:
                    if (serialPortConnected) {
                        Log.d(TAG, "Restart Connection");
                        serialPort.close();
                        connection = usbManager.openDevice(device);
                        new ConnectionThread().start();
                    }
                    break;
                default:
                    Log.e(TAG, "Unknown action");
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        this.context = this;

        serialPortConnected = false;
        UsbService.SERVICE_CONNECTED = true;
        setFilter();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        findSerialPortDevice();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UsbService.SERVICE_CONNECTED = false;
    }

    public void write(byte[] data) {
        if (serialPort != null) {
            serialPort.write(data);
        }
    }

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    private void findSerialPortDevice() {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();

        if (usbDevices.isEmpty()) {
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
            return;
        }

        boolean keep = true;
        for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
            device = entry.getValue();
            int deviceVID = device.getVendorId();
            int devicePID = device.getProductId();

            if (DBG) {
                Log.d(TAG, "VendorID: " + deviceVID + ", ProductID: " + devicePID);
            }

            if (deviceVID != 0x1d6b
                    &&(devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003)) {
                requestUserPermission();
                keep = false;
            } else {
                connection = null;
                device = null;
            }

            if (!keep) {
                break;
            }
        }

        if (!keep) {
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
        }
    }

    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        filter.addAction(ACTION_SERIAL_CONFIG_CHANGED);
        registerReceiver(usbReceiver, filter);
    }

    private void requestUserPermission() {
        PendingIntent mPendingIntent =
                PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
    }

    public class UsbBinder extends Binder {
        public UsbService getService() {
            return UsbService.this;
        }
    }

    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialPort == null) {
                Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                context.sendBroadcast(intent);
                return;
            }

            if (serialPort.open()) {
                serialPortConnected = true;

                SharedPreferences pref =
                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                serialPort.setBaudRate(Integer.parseInt(pref.getString(getString(R.string.baudrate_key),
                        getResources().getString(R.string.baudrate_default))));
                serialPort.setDataBits(Integer.parseInt(pref.getString(getString(R.string.databits_key),
                        getResources().getString(R.string.databits_default))));
                serialPort.setStopBits(Integer.parseInt(pref.getString(getString(R.string.stopbits_key),
                        getResources().getString(R.string.stopbits_default))));
                serialPort.setParity(Integer.parseInt(pref.getString(getString(R.string.parity_key),
                        getResources().getString(R.string.parity_default))));
                serialPort.setFlowControl(Integer.parseInt(pref.getString(getString(R.string.flowcontrol_key),
                        getResources().getString(R.string.flowcontrol_default))));

                serialPort.read(mCallback);
                serialPort.getCTS(ctsCallback);
                serialPort.getDSR(dsrCallback);

                Intent intent = new Intent(ACTION_USB_READY);
                context.sendBroadcast(intent);
            } else {
                if (serialPort instanceof CDCSerialDevice) {
                    Intent intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
                    context.sendBroadcast(intent);
                } else {
                    Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                    context.sendBroadcast(intent);
                }
            }
        }
    }
}
