package jp.sugnakys.usbserialconsole

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import jp.sugnakys.usbserialconsole.util.Constants
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.*

class LogViewActivity : BaseAppCompatActivity() {

    companion object {
        private const val TAG = "LogViewActivity"
    }

    private var logFile: File? = null
    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.log_view_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
            toolbar!!.setNavigationOnClickListener { finish() }
        }
    }

    override fun onResume() {
        super.onResume()
        logFile = intent.extras?.get(Constants.EXTRA_LOG_FILE) as File?
        if (logFile != null) {
            toolbar!!.title = logFile!!.name
            setLogText(logFile!!)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_log_view, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent
        if (item.itemId == R.id.action_send_to) {
            intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(Uri.fromFile(logFile), "text/plain")
            startActivity(intent)
        } else {
            Log.e(TAG, "Unknown id")
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setLogText(file: File) {
        val textView = findViewById<TextView>(R.id.logView)
        var fileInputStream: FileInputStream? = null
        try {
            fileInputStream = FileInputStream(file)
            val readBytes = ByteArray(fileInputStream.available())
            if (fileInputStream.read(readBytes) != -1) {
                val readString = String(readBytes, Charset.forName(Constants.CHARSET))
                textView.text = readString
            }
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
        } finally {
            try {
                fileInputStream?.close()
            } catch (e: IOException) {
                Log.e(TAG, e.toString())
            }
        }
    }
}
