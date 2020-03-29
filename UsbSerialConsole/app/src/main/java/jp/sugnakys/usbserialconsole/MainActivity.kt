package jp.sugnakys.usbserialconsole

import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager
import jp.sugnakys.usbserialconsole.UsbService.UsbBinder
import jp.sugnakys.usbserialconsole.settings.SettingsActivity
import jp.sugnakys.usbserialconsole.util.Constants
import jp.sugnakys.usbserialconsole.util.Log
import jp.sugnakys.usbserialconsole.util.Util
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.lang.ref.WeakReference
import java.util.*
import java.util.regex.Pattern

class MainActivity : BaseAppCompatActivity(), View.OnClickListener, TextWatcher {

    companion object {
        private const val TAG = "MainActivity"
        private const val RECEIVED_TEXT_VIEW_STR = "RECEIVED_TEXT_VIEW_STR"
    }

    private var usbService: UsbService? = null

    private val usbConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, arg1: IBinder) {
            usbService = (arg1 as UsbBinder).service
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            usbService = null
        }
    }

    private var mainLayout: LinearLayout? = null
    private var sendBtn: Button? = null
    private var sendMsgView: EditText? = null
    private var sendViewLayout: LinearLayout? = null
    private var receivedMsgView: TextView? = null
    private var scrollView: ScrollView? = null
    private var mOptionMenu: Menu? = null
    private var mHandler: MyHandler? = null
    private var timestampFormat: String? = null
    private var lineFeedCode: String? = null
    private var tmpReceivedData = ""
    private var showTimeStamp = false
    private var isUSBReady = false
    private var isConnect = false

    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (Objects.requireNonNull(intent.action)) {
                UsbService.ACTION_USB_PERMISSION_GRANTED -> {
                    Toast.makeText(context,
                            getString(R.string.usb_permission_granted),
                            Toast.LENGTH_SHORT).show()
                    isUSBReady = true
                    updateOptionsMenu()
                    requestConnection()
                }
                UsbService.ACTION_USB_PERMISSION_NOT_GRANTED -> Toast.makeText(context,
                        getString(R.string.usb_permission_not_granted),
                        Toast.LENGTH_SHORT).show()
                UsbService.ACTION_NO_USB -> Toast.makeText(context,
                        getString(R.string.no_usb),
                        Toast.LENGTH_SHORT).show()
                UsbService.ACTION_USB_DISCONNECTED -> {
                    Toast.makeText(context,
                            getString(R.string.usb_disconnected),
                            Toast.LENGTH_SHORT).show()
                    isUSBReady = false
                    stopConnection()
                }
                UsbService.ACTION_USB_NOT_SUPPORTED -> Toast.makeText(context, getString(R.string.usb_not_supported),
                        Toast.LENGTH_SHORT).show()
                else -> Log.e(TAG, "Unknown action")
            }
        }
    }

    private fun requestConnection() {
        AlertDialog.Builder(this@MainActivity)
                .setMessage(getString(R.string.confirm_connect))
                .setPositiveButton(getString(android.R.string.ok)) { _: DialogInterface?, _: Int -> startConnection() }
                .setNegativeButton(getString(android.R.string.cancel), null)
                .create()
                .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.AppTheme_NoActionBar)

        mHandler = MyHandler(this)
        setContentView(R.layout.activity_main)

        mainLayout = findViewById(R.id.mainLayout)
        receivedMsgView = findViewById(R.id.receivedMsgView)
        scrollView = findViewById(R.id.scrollView)
        sendBtn = findViewById(R.id.sendBtn)
        sendMsgView = findViewById(R.id.sendMsgView)
        sendViewLayout = findViewById(R.id.sendViewLayout)

        sendBtn!!.setOnClickListener(this)
        sendMsgView!!.addTextChangedListener(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    private fun setDefaultColor() {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = pref.edit()
        if (!pref.contains(getString(R.string.color_console_background_key))) {
            var defaultBackgroundColor = Color.TRANSPARENT
            val background = mainLayout!!.background
            if (background is ColorDrawable) {
                defaultBackgroundColor = background.color
            }
            editor.putInt(getString(R.string.color_console_background_key), defaultBackgroundColor)
            editor.apply()
            Log.d(TAG, "Default background color: " + String.format("#%08X", defaultBackgroundColor))
        }
        if (!pref.contains(getString(R.string.color_console_text_key))) {
            val defaultTextColor = receivedMsgView!!.textColors.defaultColor
            editor.putInt(getString(R.string.color_console_text_key), defaultTextColor)
            editor.apply()
            Log.d(TAG, "Default text color: " + String.format("#%08X", defaultTextColor))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(RECEIVED_TEXT_VIEW_STR, receivedMsgView!!.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        receivedMsgView!!.text = savedInstanceState.getString(RECEIVED_TEXT_VIEW_STR)
    }

    override fun onResume() {
        super.onResume()
        setDefaultColor()

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        showTimeStamp = pref.getBoolean(
                resources.getString(R.string.timestamp_visible_key), true)

        timestampFormat = pref.getString(getString(R.string.timestamp_format_key),
                getString(R.string.timestamp_format_default))

        lineFeedCode = Util.getLineFeedCd(
                pref.getString(getString(R.string.line_feed_code_send_key),
                        getString(R.string.line_feed_code_cr_lf_value)),
                this)

        if (pref.getBoolean(getString(R.string.send_form_visible_key), true)) {
            sendViewLayout!!.visibility = View.VISIBLE
        } else {
            sendViewLayout!!.visibility = View.GONE
        }

        if (pref.getBoolean(getString(R.string.sleep_mode_key), false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        val backgroundColor = pref.getInt(getString(R.string.color_console_background_key), Color.WHITE)
        Log.d(TAG, "Background color: " + String.format("#%08X", backgroundColor))
        mainLayout!!.setBackgroundColor(backgroundColor)

        val textColor = pref.getInt(getString(R.string.color_console_text_key), Color.BLACK)
        Log.d(TAG, "Text color: " + String.format("#%08X", textColor))
        receivedMsgView!!.setTextColor(textColor)
        sendMsgView!!.setTextColor(textColor)

        setFilters()

        startService(usbConnection)

        updateOptionsMenu()
    }

    public override fun onDestroy() {
        if (isConnect) {
            stopConnection()
        }
        unregisterReceiver(mUsbReceiver)
        unbindService(usbConnection)
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder(this@MainActivity)
                    .setMessage(getString(R.string.confirm_finish_text))
                    .setPositiveButton(getString(android.R.string.ok)) { _: DialogInterface?, _: Int -> finish() }
                    .setNegativeButton(getString(android.R.string.cancel), null)
                    .create().show()
            return true
        }
        return false
    }

    private fun startService(serviceConnection: ServiceConnection) {
        if (!UsbService.SERVICE_CONNECTED) {
            val startService = Intent(this, UsbService::class.java)
            startService(startService)
        }
        val bindingIntent = Intent(this, UsbService::class.java)
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setFilters() {
        val filter = IntentFilter()
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED)
        filter.addAction(UsbService.ACTION_NO_USB)
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED)
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED)
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED)
        registerReceiver(mUsbReceiver, filter)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        sendBtn!!.isEnabled = s.isNotEmpty()
    }

    override fun afterTextChanged(s: Editable) {}
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        mOptionMenu = menu
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private fun updateOptionsMenu() {
        if (mOptionMenu != null) {
            onPrepareOptionsMenu(mOptionMenu!!)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.action_connect)
        item.isEnabled = isUSBReady
        item.title = if (isConnect) {
            getString(R.string.action_disconnect)
        } else {
            getString(R.string.action_connect)
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent
        when (item.itemId) {
            R.id.action_connect -> {
                android.util.Log.d(TAG, "Connect clicked")
                if (isConnect) {
                    stopConnection()
                } else {
                    startConnection()
                }
            }
            R.id.action_clear_log -> {
                Log.d(TAG, "Clear log clicked")
                receivedMsgView!!.text = ""
            }
            R.id.action_save_log -> {
                Log.d(TAG, "Save log clicked")
                writeToFile(receivedMsgView!!.text.toString())
            }
            R.id.action_settings -> {
                Log.d(TAG, "Settings clicked")
                intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.action_log_list -> {
                Log.d(TAG, "Log list clicked")
                intent = Intent(this, LogListViewActivity::class.java)
                startActivity(intent)
            }
            else -> Log.e(TAG, "Unknown id")
        }
        return super.onOptionsItemSelected(item)
    }

    private fun writeToFile(data: String) {
        var fos: FileOutputStream? = null
        try {
            val fileName = Util.getCurrentDateForFile() + Constants.LOG_EXT
            val dirName = Util.getLogDir(applicationContext)
            val outputFile = File(dirName, fileName)
            fos = FileOutputStream(outputFile)
            fos.write(data.toByteArray(charset(Constants.CHARSET)))
            Log.d(TAG, "Save: " + outputFile.canonicalPath)
            val toastStr: String
            toastStr = if (Util.isInternalDir(applicationContext, dirName)) {
                getString(R.string.action_save_log) + " : " + fileName
            } else {
                getString(R.string.action_save_log) + " : " + outputFile.canonicalPath
            }
            Toast.makeText(this, toastStr, Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
        } finally {
            try {
                fos?.close()
            } catch (e: IOException) {
                Log.e(TAG, e.toString())
            }
        }
    }

    private fun sendMessage(msg: String) {
        val pattern = Pattern.compile("\n$")
        val matcher = pattern.matcher(msg)
        val strResult = matcher.replaceAll("") + lineFeedCode
        try {
            usbService!!.write(strResult.toByteArray(charset(Constants.CHARSET)))
            Log.d(TAG, "SendMessage: $msg")
            addReceivedData(msg)
        } catch (e: UnsupportedEncodingException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun startConnection() {
        usbService!!.setHandler(mHandler)
        isConnect = true
        Toast.makeText(applicationContext,
                getString(R.string.start_connection), Toast.LENGTH_SHORT).show()
        updateOptionsMenu()
    }

    private fun stopConnection() {
        usbService!!.setHandler(null)
        isConnect = false
        Toast.makeText(applicationContext,
                getString(R.string.stop_connection), Toast.LENGTH_SHORT).show()
        updateOptionsMenu()
    }

    private fun addReceivedData(data: String) {
        if (showTimeStamp) {
            addReceivedDataWithTime(data)
        } else {
            addTextView(data)
        }
    }

    private fun addTextView(data: String) {
        receivedMsgView!!.append(data)
        scrollView!!.scrollTo(0, receivedMsgView!!.bottom)
    }

    private fun addReceivedDataWithTime(data: String) {
        val timeStamp = "[" + Util.getCurrentTime(timestampFormat) + "] "
        tmpReceivedData += data
        val separateStr = getLineSeparater(tmpReceivedData)
        if (separateStr.isNotEmpty()) {
            val strArray = tmpReceivedData.split(separateStr).toTypedArray()
            tmpReceivedData = ""
            for (i in strArray.indices) {
                if (strArray.size != 1 && i == strArray.size - 1 && strArray[i].isNotEmpty()) {
                    tmpReceivedData = strArray[i]
                } else {
                    addTextView(timeStamp + strArray[i] + System.lineSeparator())
                }
            }
        }
    }

    private fun getLineSeparater(str: String): String {
        return when {
            str.contains(Constants.CR_LF) -> Constants.CR_LF
            str.contains(Constants.LF) -> Constants.LF
            str.contains(Constants.CR) -> Constants.CR
            else -> ""
        }
    }

    override fun onClick(view: View) {
        if (view.id == R.id.sendBtn) {
            android.util.Log.d(TAG, "Send button clicked")
            var message = sendMsgView!!.text.toString()
            if (message.isNotEmpty()) {
                message += System.lineSeparator()
                sendMessage(message)
                sendMsgView!!.setText("")
            }
        }
        android.util.Log.e(TAG, "Unknown view")
    }

    private class MyHandler(activity: MainActivity) : Handler() {
        private val mActivity: WeakReference<MainActivity> = WeakReference(activity)
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                UsbService.MESSAGE_FROM_SERIAL_PORT -> {
                    val data = msg.obj as String
                    mActivity.get()!!.addReceivedData(data)
                }
                UsbService.CTS_CHANGE -> {
                    Log.d(TAG, "CTS_CHANGE")
                    Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show()
                }
                UsbService.DSR_CHANGE -> {
                    Log.d(TAG, "DSR_CHANGE")
                    Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show()
                }
                else -> Log.e(TAG, "Unknown message")
            }
        }

    }
}
