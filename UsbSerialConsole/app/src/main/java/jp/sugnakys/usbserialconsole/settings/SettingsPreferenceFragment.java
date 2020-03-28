package jp.sugnakys.usbserialconsole.settings;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import java.util.Objects;

import jp.sugnakys.usbserialconsole.R;

public class SettingsPreferenceFragment extends BasePreferenceFragment
        implements Preference.OnPreferenceClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fragment_settings_preference);

        String[] prefKeys = new String[]{
                getString(R.string.serial_port_key),
                getString(R.string.display_key),
                getString(R.string.connection_key),
                getString(R.string.log_key),
                getString(R.string.license_key)};

        for (String prefKey : prefKeys) {
            findPreference(prefKey).setOnPreferenceClickListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Toolbar toolbar = Objects.requireNonNull(getActivity()).findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.action_settings));
        toolbar.setNavigationOnClickListener(view -> getActivity().finish());
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        String simpleName = null;
        Fragment fragment = null;

        if (key.equals(getString(R.string.serial_port_key))) {
            simpleName = SerialPortPreferenceFragment.class.getSimpleName();
            fragment = new SerialPortPreferenceFragment();
        } else if (key.equals(getString(R.string.display_key))) {
            simpleName = DisplayPreferenceFragment.class.getSimpleName();
            fragment = new DisplayPreferenceFragment();
        } else if (key.equals(getString(R.string.connection_key))) {
            simpleName = ConnectionPreferenceFragment.class.getSimpleName();
            fragment = new ConnectionPreferenceFragment();
        } else if (key.equals(getString(R.string.log_key))) {
            simpleName = LogPreferenceFragment.class.getSimpleName();
            fragment = new LogPreferenceFragment();
        } else if (key.equals(getString(R.string.license_key))) {
            DialogFragment licenseFragment = new LicenseDialogFragment();
            licenseFragment.show(Objects.requireNonNull(getFragmentManager()), LicenseDialogFragment.class.getSimpleName());
        }

        if (simpleName != null) {
            Objects.requireNonNull(getFragmentManager())
                    .beginTransaction()
                    .addToBackStack(simpleName)
                    .replace(R.id.content_frame, fragment, simpleName)
                    .commit();
        }
        return false;
    }
}
