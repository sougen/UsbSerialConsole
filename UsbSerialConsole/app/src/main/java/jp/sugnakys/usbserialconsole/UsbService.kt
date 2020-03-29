package jp.sugnakys.usbserialconsole

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.preference.PreferenceManager
import com.felhr.usbserial.CDCSerialDevice
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface.*
import jp.sugnakys.usbserialconsole.util.Constants
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*

class UsbService : Service() {

    companion object {
        private const val TAG = "UsbService"
        private const val DBG = true
        const val ACTION_USB_NOT_SUPPORTED = "jp.sugnakys.usbserialconsole.USB_NOT_SUPPORTED"
        const val ACTION_NO_USB = "jp.sugnakys.usbserialconsole.NO_USB"
        const val ACTION_USB_PERMISSION_GRANTED = "jp.sugnakys.usbserialconsole.USB_PERMISSION_GRANTED"
        const val ACTION_USB_PERMISSION_NOT_GRANTED = "jp.sugnakys.usbserialconsole.USB_PERMISSION_NOT_GRANTED"
        const val ACTION_USB_DISCONNECTED = "jp.sugnakys.usbserialconsole.USB_DISCONNECTED"
        const val ACTION_SERIAL_CONFIG_CHANGED = "jp.sugnakys.usbserialconsole.SERIAL_CONFIG_CHANGED"
        private const val ACTION_USB_READY = "jp.sugnakys.usbserialconsole.USB_READY"
        private const val ACTION_CDC_DRIVER_NOT_WORKING = "jp.sugnakys.usbserialconsole.ACTION_CDC_DRIVER_NOT_WORKING"
        private const val ACTION_USB_DEVICE_NOT_WORKING = "jp.sugnakys.usbserialconsole.ACTION_USB_DEVICE_NOT_WORKING"
        private const val ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
        private const val ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"
        private const val ACTION_USB_PERMISSION = "jp.sugnakys.usbserialconsole.USB_PERMISSION"
        const val MESSAGE_FROM_SERIAL_PORT = 0
        const val CTS_CHANGE = 1
        const val DSR_CHANGE = 2
        var SERVICE_CONNECTED = false
    }

    private val binder: IBinder = UsbBinder()
    private var context: Context? = null
    private var mHandler: Handler? = null

    private val mCallback = UsbReadCallback { arg ->
        try {
            val data = String(arg, Charset.forName(Constants.CHARSET))
            if (mHandler != null) {
                mHandler!!.obtainMessage(MESSAGE_FROM_SERIAL_PORT, data).sendToTarget()
            }
        } catch (e: UnsupportedEncodingException) {
            Log.d(TAG, e.toString())
        }
    }
    private val ctsCallback = UsbCTSCallback {
        if (mHandler != null) {
            mHandler!!.obtainMessage(CTS_CHANGE).sendToTarget()
        }
    }
    private val dsrCallback = UsbDSRCallback {
        if (mHandler != null) {
            mHandler!!.obtainMessage(DSR_CHANGE).sendToTarget()
        }
    }

    private var usbManager: UsbManager? = null
    private var device: UsbDevice? = null
    private var connection: UsbDeviceConnection? = null
    private var serialPort: UsbSerialDevice? = null
    private var serialPortConnected = false

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (Objects.requireNonNull(intent.action)) {
                ACTION_USB_PERMISSION -> {
                    val granted = intent.extras?.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                    if (granted!!) {
                        val broadCast = Intent(ACTION_USB_PERMISSION_GRANTED)
                        context.sendBroadcast(broadCast)
                        connection = usbManager!!.openDevice(device)
                        ConnectionThread().start()
                    } else {
                        val broadCast = Intent(ACTION_USB_PERMISSION_NOT_GRANTED)
                        context.sendBroadcast(broadCast)
                    }
                }
                ACTION_USB_ATTACHED -> if (!serialPortConnected) {
                    findSerialPortDevice()
                }
                ACTION_USB_DETACHED -> {
                    val `in` = Intent(ACTION_USB_DISCONNECTED)
                    context.sendBroadcast(`in`)
                    if (serialPortConnected) {
                        serialPort!!.close()
                    }
                    serialPortConnected = false
                }
                ACTION_SERIAL_CONFIG_CHANGED -> if (serialPortConnected) {
                    Log.d(TAG, "Restart Connection")
                    serialPort!!.close()
                    connection = usbManager!!.openDevice(device)
                    ConnectionThread().start()
                }
                else -> Log.e(TAG, "Unknown action")
            }
        }
    }

    override fun onCreate() {
        context = this
        serialPortConnected = false
        SERVICE_CONNECTED = true
        setFilter()
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        findSerialPortDevice()
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        SERVICE_CONNECTED = false
    }

    fun write(data: ByteArray?) {
        if (serialPort != null) {
            serialPort!!.write(data)
        }
    }

    fun setHandler(mHandler: Handler?) {
        this.mHandler = mHandler
    }

    private fun findSerialPortDevice() {
        val usbDevices = usbManager!!.deviceList
        if (usbDevices.isEmpty()) {
            val intent = Intent(ACTION_NO_USB)
            sendBroadcast(intent)
            return
        }
        var keep = true
        for ((_, value) in usbDevices) {
            device = value
            val deviceVID = device!!.vendorId
            val devicePID = device!!.productId
            if (DBG) {
                Log.d(TAG, "VendorID: $deviceVID, ProductID: $devicePID")
            }
            if (deviceVID != 0x1d6b
                    && devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003) {
                requestUserPermission()
                keep = false
            } else {
                connection = null
                device = null
            }
            if (!keep) {
                break
            }
        }
        if (!keep) {
            val intent = Intent(ACTION_NO_USB)
            sendBroadcast(intent)
        }
    }

    private fun setFilter() {
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(ACTION_USB_DETACHED)
        filter.addAction(ACTION_USB_ATTACHED)
        filter.addAction(ACTION_SERIAL_CONFIG_CHANGED)
        registerReceiver(usbReceiver, filter)
    }

    private fun requestUserPermission() {
        val mPendingIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        usbManager!!.requestPermission(device, mPendingIntent)
    }

    inner class UsbBinder : Binder() {
        val service: UsbService
            get() = this@UsbService
    }

    private inner class ConnectionThread : Thread() {
        override fun run() {
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection)
            if (serialPort == null) {
                val intent = Intent(ACTION_USB_NOT_SUPPORTED)
                context!!.sendBroadcast(intent)
                return
            }
            if (serialPort!!.open()) {
                serialPortConnected = true
                val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                serialPort!!.setBaudRate(pref.getString(getString(R.string.baudrate_key),
                        resources.getString(R.string.baudrate_default))!!.toInt())
                serialPort!!.setDataBits(pref.getString(getString(R.string.databits_key),
                        resources.getString(R.string.databits_default))!!.toInt())
                serialPort!!.setStopBits(pref.getString(getString(R.string.stopbits_key),
                        resources.getString(R.string.stopbits_default))!!.toInt())
                serialPort!!.setParity(pref.getString(getString(R.string.parity_key),
                        resources.getString(R.string.parity_default))!!.toInt())
                serialPort!!.setFlowControl(pref.getString(getString(R.string.flowcontrol_key),
                        resources.getString(R.string.flowcontrol_default))!!.toInt())
                serialPort!!.read(mCallback)
                serialPort!!.getCTS(ctsCallback)
                serialPort!!.getDSR(dsrCallback)
                val intent = Intent(ACTION_USB_READY)
                context!!.sendBroadcast(intent)
            } else {
                if (serialPort is CDCSerialDevice) {
                    val intent = Intent(ACTION_CDC_DRIVER_NOT_WORKING)
                    context!!.sendBroadcast(intent)
                } else {
                    val intent = Intent(ACTION_USB_DEVICE_NOT_WORKING)
                    context!!.sendBroadcast(intent)
                }
            }
        }
    }
}
