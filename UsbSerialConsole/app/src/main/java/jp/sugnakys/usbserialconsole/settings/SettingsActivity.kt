package jp.sugnakys.usbserialconsole.settings

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import jp.sugnakys.usbserialconsole.BaseAppCompatActivity
import jp.sugnakys.usbserialconsole.R
import jp.sugnakys.usbserialconsole.util.Log

class SettingsActivity : BaseAppCompatActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
    }

    private var mPermissionResultListener: PermissionResultListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings_main)

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.content_frame, SettingsPreferenceFragment())
        fragmentTransaction.commit()
    }

    override fun onResume() {
        super.onResume()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
        }
    }

    fun setPermissionResultListener(mPermissionResultListener: PermissionResultListener?) {
        this.mPermissionResultListener = mPermissionResultListener
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.d(TAG, "onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (mPermissionResultListener != null) {
            mPermissionResultListener!!.onPermissionResult(requestCode, permissions, grantResults)
        }
    }
}
