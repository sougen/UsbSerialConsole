package jp.sugnakys.usbserialconsole.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import jp.sugnakys.usbserialconsole.R
import jp.sugnakys.usbserialconsole.UsbService
import jp.sugnakys.usbserialconsole.util.Log

class SerialPortPreferenceFragment : BasePreferenceFragment() {

    companion object {
        private const val TAG = "SerialPortPreferenceFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.fragment_serial_port_preference)

        listPrefKeys = arrayOf(
                getString(R.string.baudrate_key), getString(R.string.databits_key),
                getString(R.string.parity_key), getString(R.string.stopbits_key),
                getString(R.string.flowcontrol_key))
    }

    override fun onResume() {
        super.onResume()
        val toolbar: Toolbar = activity!!.findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.serial_port_title)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        super.onSharedPreferenceChanged(sharedPreferences, key)

        Log.d(TAG, "Restart UsbService")
        val intent = Intent(UsbService.ACTION_SERIAL_CONFIG_CHANGED)
        activity!!.sendBroadcast(intent)
    }
}
