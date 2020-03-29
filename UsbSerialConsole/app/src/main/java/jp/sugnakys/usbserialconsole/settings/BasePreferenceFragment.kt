package jp.sugnakys.usbserialconsole.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import jp.sugnakys.usbserialconsole.util.Log

open class BasePreferenceFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {

    lateinit var sharedPreference: SharedPreferences

    @JvmField
    var listPrefKeys: Array<String>? = null

    companion object {
        private const val TAG = "BasePreferenceFragment"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Noting to do
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val preference = findPreference<Preference>(key)
        if (preference is ListPreference) {
            preference.setSummary(preference.entry)
        }
    }

    override fun onResume() {
        super.onResume()

        sharedPreference = preferenceManager.sharedPreferences
        sharedPreference.registerOnSharedPreferenceChangeListener(this)

        setSummary()
    }

    private fun setSummary() {
        listPrefKeys?.let {
            for (prefKey in it) {
                val listPref = findPreference<ListPreference>(prefKey)
                Log.d(TAG, "Preference: " + prefKey + ", value: " + listPref?.entry)
                listPref?.summary = listPref?.entry ?: ""
            }
        }
    }

    override fun onPause() {
        sharedPreference.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }
}
