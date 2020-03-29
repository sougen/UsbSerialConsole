package jp.sugnakys.usbserialconsole

import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import jp.sugnakys.usbserialconsole.util.Util

open class BaseAppCompatActivity : AppCompatActivity() {
    public override fun onResume() {
        super.onResume()

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val screenOrientation = pref.getString(getString(R.string.screen_orientation_key),
                getString(R.string.screen_orientation_default))
        Util.setScreenOrientation(screenOrientation, this)
    }
}
