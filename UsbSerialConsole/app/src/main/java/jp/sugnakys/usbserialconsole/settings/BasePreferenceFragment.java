package jp.sugnakys.usbserialconsole.settings;

import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;

import androidx.appcompat.widget.Toolbar;

import jp.sugnakys.usbserialconsole.R;
import jp.sugnakys.usbserialconsole.util.Log;

public class BasePreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "BasePreferenceFragment";

    SharedPreferences sharedPreference;

    String[] listPrefKeys;

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

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager().popBackStack();
            }
        });
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
