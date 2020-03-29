package jp.sugnakys.usbserialconsole.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import jp.sugnakys.usbserialconsole.R

class SettingsPreferenceFragment : BasePreferenceFragment(), Preference.OnPreferenceClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.fragment_settings_preference)

        val prefKeys = arrayOf(
                getString(R.string.serial_port_key),
                getString(R.string.display_key),
                getString(R.string.connection_key),
                getString(R.string.log_key),
                getString(R.string.license_key))
        for (prefKey in prefKeys) {
            findPreference<Preference>(prefKey)?.onPreferenceClickListener = this
        }
    }

    override fun onResume() {
        super.onResume()

        val toolbar: Toolbar? = view?.findViewById(R.id.toolbar)
        toolbar?.title = getString(R.string.action_settings)
        toolbar?.setNavigationOnClickListener { _: View? -> activity!!.finish() }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val key = preference.key
        lateinit var simpleName: String
        lateinit var fragment: Fragment

        when (key) {
            getString(R.string.serial_port_key) -> {
                simpleName = SerialPortPreferenceFragment::class.java.simpleName
                fragment = SerialPortPreferenceFragment()
            }
            getString(R.string.display_key) -> {
                simpleName = DisplayPreferenceFragment::class.java.simpleName
                fragment = DisplayPreferenceFragment()
            }
            getString(R.string.connection_key) -> {
                simpleName = ConnectionPreferenceFragment::class.java.simpleName
                fragment = ConnectionPreferenceFragment()
            }
            getString(R.string.log_key) -> {
                simpleName = LogPreferenceFragment::class.java.simpleName
                fragment = LogPreferenceFragment()
            }
            getString(R.string.license_key) -> {
                val licenseFragment: DialogFragment = LicenseDialogFragment()
                licenseFragment.show(fragmentManager!!, LicenseDialogFragment::class.java.simpleName)
                return false
            }
            else -> return false
        }

        fragmentManager!!
                .beginTransaction()
                .addToBackStack(simpleName)
                .replace(R.id.content_frame, fragment, simpleName)
                .commit()

        return false
    }
}
