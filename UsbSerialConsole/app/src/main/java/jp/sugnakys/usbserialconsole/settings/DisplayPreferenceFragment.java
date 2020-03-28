package jp.sugnakys.usbserialconsole.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.preference.ListPreference;

import java.util.Objects;

import jp.sugnakys.usbserialconsole.R;
import jp.sugnakys.usbserialconsole.util.Util;

public class DisplayPreferenceFragment extends BasePreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fragment_display_preference);

        listPrefKeys = new String[]{getString(R.string.screen_orientation_key)};
    }

    @Override
    public void onResume() {
        super.onResume();

        Toolbar toolbar = Objects.requireNonNull(getActivity()).findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.display_title));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        super.onSharedPreferenceChanged(sharedPreferences, key);

        if (key.equals(getString(R.string.screen_orientation_key))) {
            Util.setScreenOrientation(
                    ((ListPreference) findPreference(key)).getValue(),
                    Objects.requireNonNull(getActivity()));
        }
    }
}
