package jp.sugnakys.usbserialconsole.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import jp.sugnakys.usbserialconsole.R;
import jp.sugnakys.usbserialconsole.util.Log;

public class BasePreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "BasePreferenceFragment";

    SharedPreferences sharedPreference;

    String[] listPrefKeys;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Noting to do
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            preference.setSummary(listPref.getEntry());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        sharedPreference = getPreferenceManager().getSharedPreferences();
        sharedPreference.registerOnSharedPreferenceChangeListener(this);
        setSummary();

        Toolbar toolbar = Objects.requireNonNull(getActivity()).findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> Objects.requireNonNull(getFragmentManager()).popBackStack());
    }

    private void setSummary() {
        if (listPrefKeys == null) {
            return;
        }

        ListPreference listPref;
        for (String prefKey : listPrefKeys) {
            listPref = (ListPreference) findPreference(prefKey);
            Log.d(TAG, "Preference: " + prefKey + ", value: " + listPref.getEntry());
            listPref.setSummary(listPref.getEntry());
        }
    }

    @Override
    public void onPause() {
        sharedPreference.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }
}
