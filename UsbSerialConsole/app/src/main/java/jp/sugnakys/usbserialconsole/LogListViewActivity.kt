package jp.sugnakys.usbserialconsole

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.widget.Toolbar
import jp.sugnakys.usbserialconsole.util.Constants
import jp.sugnakys.usbserialconsole.util.Util
import java.io.File

class LogListViewActivity : BaseAppCompatActivity(),
        OnItemClickListener, OnItemLongClickListener {

    companion object {
        private const val TAG = "LogListViewActivity"
    }

    private var listView: ListView? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.log_list_view_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.action_log_list)

        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
            toolbar.setNavigationOnClickListener { finish() }
        }
        listView = findViewById(R.id.listView)
        listView!!.onItemClickListener = this
        listView!!.onItemLongClickListener = this
    }

    override fun onResume() {
        super.onResume()
        updateList()
    }

    private val fileNameList: Array<String?>?
        get() {
            val file = Util.getLogDir(applicationContext).listFiles()
            if (file == null) {
                Log.w(TAG, "File not found")
                return null
            }
            val fileName = arrayOfNulls<String>(file.size)
            for (i in file.indices) {
                fileName[i] = file[i].name
            }
            return fileName
        }

    private fun updateList() {
        val files = fileNameList
        if (files == null) {
            Log.w(TAG, "File not found")
            return
        }
        val adapter = ArrayAdapter(this,
                android.R.layout.simple_list_item_1,
                files)
        listView!!.adapter = adapter
    }

    override fun onItemClick(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
        val listView = adapterView as ListView
        val selectedItem = listView.getItemAtPosition(position) as String
        val targetFile = File(Util.getLogDir(applicationContext), selectedItem)
        val intent = Intent(applicationContext, LogViewActivity::class.java)
        intent.putExtra(Constants.EXTRA_LOG_FILE, targetFile)
        startActivity(intent)
    }

    override fun onItemLongClick(adapterView: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
        val listView = adapterView as ListView
        val selectedItem = listView.getItemAtPosition(position) as String
        AlertDialog.Builder(this@LogListViewActivity)
                .setTitle(resources.getString(R.string.delete_log_file_title))
                .setMessage(resources.getString(R.string.delete_log_file_text) + "\n"
                        + resources.getString(R.string.file_name) + ": " + selectedItem)
                .setPositiveButton(android.R.string.ok
                ) { _: DialogInterface?, _: Int ->
                    val context = applicationContext
                    val targetFile = File(Util.getLogDir(context), selectedItem)
                    Log.d(TAG, "Delete file path: " + targetFile.name)
                    if (targetFile.delete()) {
                        updateList()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        return true
    }
}
