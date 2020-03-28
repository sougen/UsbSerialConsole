package jp.sugnakys.usbserialconsole;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

import jp.sugnakys.usbserialconsole.util.Util;

@SuppressLint("Registered")
public class BaseAppCompatActivity extends AppCompatActivity {

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

        String screenOrientation = pref.getString(getString(R.string.screen_orientation_key),
                getString(R.string.screen_orientation_default));

        Util.setScreenOrientation(screenOrientation, this);
    }
}
