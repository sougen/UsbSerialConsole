package jp.sugnakys.usbserialconsole.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;

import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import jp.sugnakys.usbserialconsole.R;
import jp.sugnakys.usbserialconsole.util.Log;

public class LogPreferenceFragment extends BasePreferenceFragment
        implements Preference.OnPreferenceClickListener, DirectoryChooserFragment.OnFragmentInteractionListener {

    private static final String TAG = "LogPreferenceFragment";

    private SwitchPreference switchStoragePreference;
    private Preference saveLocationPreference;
    private DirectoryChooserFragment mDialog;

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

        switchStoragePreference = (SwitchPreference) findPreference(getString(R.string.log_switch_storage_key));
        saveLocationPreference = (Preference) findPreference(getString(R.string.log_directory_key));
    }

    @Override
    public void onResume() {
        super.onResume();

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.log_title));

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        saveLocationPreference.setSummary(pref.getString(getString(R.string.log_directory_key), android.os.Environment.getExternalStorageDirectory().getAbsolutePath()));
        setSaveLocationEnable(switchStoragePreference.isChecked());
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        if (key.equals(getString(R.string.log_switch_storage_key))) {
        } else if (key.equals(getString(R.string.log_directory_key))) {
            Log.d(TAG, android.os.Environment.getExternalStorageDirectory().getAbsolutePath());
            final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                    .initialDirectory(android.os.Environment.getExternalStorageDirectory().getAbsolutePath())
                    .newDirectoryName("New Directory")
                    .allowNewDirectoryNameModification(true)
                    .build();

            mDialog = DirectoryChooserFragment.newInstance(config);
            mDialog.setTargetFragment(this, 0);
            mDialog.show(getFragmentManager(), null);
        }

        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        super.onSharedPreferenceChanged(sharedPreferences, key);

        if (key.equals(getString(R.string.log_switch_storage_key))) {
            setSaveLocationEnable(((SwitchPreference) findPreference(key)).isChecked());
        }
    }

    private void setSaveLocationEnable(boolean isEnable) {
        saveLocationPreference.setEnabled(isEnable);
    }

    @Override
    public void onSelectDirectory(@NonNull String path) {
        mDialog.dismiss();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(getString(R.string.log_directory_key), path);
        editor.apply();

        saveLocationPreference.setSummary(path);
    }

    @Override
    public void onCancelChooser() {
        mDialog.dismiss();
    }
}
