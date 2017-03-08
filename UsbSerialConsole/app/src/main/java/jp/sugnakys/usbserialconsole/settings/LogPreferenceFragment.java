package jp.sugnakys.usbserialconsole.settings;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;

import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import jp.sugnakys.usbserialconsole.R;
import jp.sugnakys.usbserialconsole.util.Log;

public class LogPreferenceFragment extends BasePreferenceFragment
        implements Preference.OnPreferenceClickListener, DirectoryChooserFragment.OnFragmentInteractionListener, PermissionResultListener {

    private static final String TAG = "LogPreferenceFragment";

    private final int REQUEST_CODE_WRITE_STORAGE_PERMISSION = 1;

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
        saveLocationPreference = findPreference(getString(R.string.log_directory_key));
    }

    @Override
    public void onResume() {
        super.onResume();

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.log_title));

        checkStoragePermission();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (!pref.contains(getString(R.string.log_directory_key))) {
            setSaveLocation(android.os.Environment.getExternalStorageDirectory().getAbsolutePath());
        }
        saveLocationPreference.setSummary(pref.getString(getString(R.string.log_directory_key), android.os.Environment.getExternalStorageDirectory().getAbsolutePath()));
        setSaveLocationEnable(switchStoragePreference.isChecked());
    }

    private void checkStoragePermission() {
        int permission = PermissionChecker.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            switchStoragePreference.setChecked(true);
        } else {
            switchStoragePreference.setChecked(false);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        if (key.equals(getString(R.string.log_switch_storage_key))) {
            if (switchStoragePreference.isChecked()) {
                int permissionCheck = PermissionChecker.checkSelfPermission(getActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    requestStoragePermission();
                }
            }
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

    private void requestStoragePermission() {
        Log.d(TAG, "requestStoragePermission");

        Activity activity = getActivity();
        if (activity != null && activity instanceof SettingsActivity) {
            SettingsActivity settingsActivity = (SettingsActivity) activity;
            ActivityCompat.requestPermissions(settingsActivity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_STORAGE_PERMISSION);
            settingsActivity.setPermissionResultListener(this);
        }
    }

    public void onPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onPermissionResult");
        switch (requestCode) {
            case REQUEST_CODE_WRITE_STORAGE_PERMISSION:
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "onRequestPermissionsResult: GRANTED");
                    } else {
                        Log.d(TAG, "onRequestPermissionsResult: DENIED");
                        switchStoragePreference.setChecked(false);

                        boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permissions[i]);
                        if (!showRationale) {
                            // User choose "Don't ask again"
                            new AlertDialog.Builder(getActivity())
                                    .setMessage(getString(R.string.permission_storage_message))
                                    .setPositiveButton(android.R.string.ok, null)
                                    .setNeutralButton(R.string.permission_storeage_open_settings,
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    openSettings();
                                                }
                                            })
                                    .create()
                                    .show();
                        } else if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[i])) {
                            new AlertDialog.Builder(getActivity())
                                    .setMessage(getString(R.string.permission_storage_message))
                                    .setPositiveButton(android.R.string.ok, null)
                                    .create()
                                    .show();
                        }
                    }
                }
                break;
            default:
                Log.e(TAG, "Unknown request: " + requestCode);
                break;
        }
    }

    private void openSettings() {
        Log.d(TAG, "openSettings");
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
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

        setSaveLocation(path);
        saveLocationPreference.setSummary(path);
    }

    private void setSaveLocation(String path) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(getString(R.string.log_directory_key), path);
        editor.apply();
    }

    @Override
    public void onCancelChooser() {
        mDialog.dismiss();
    }
}
