package jp.sugnakys.usbserialconsole.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.widget.Toolbar;

import jp.sugnakys.usbserialconsole.R;

public class LogPreferenceFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fragment_log_preference);

        String[] prefKeys = new String[]{
                getString(R.string.log_switch_storage_key),
                getString(R.string.log_directory_key)};

        for (String prefKey : prefKeys) {
            findPreference(prefKey).setOnPreferenceClickListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.log_title));
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        if (key.equals(getString(R.string.log_switch_storage_key))) {
        } else if (key.equals(getString(R.string.log_directory_key))) {
        }

        return false;
    }

}
