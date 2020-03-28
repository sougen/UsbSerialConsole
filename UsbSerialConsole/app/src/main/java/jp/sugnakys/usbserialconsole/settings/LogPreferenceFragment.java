package jp.sugnakys.usbserialconsole.settings;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import java.util.Objects;

import jp.sugnakys.usbserialconsole.R;
import jp.sugnakys.usbserialconsole.util.Log;

public class LogPreferenceFragment extends BasePreferenceFragment
        implements Preference.OnPreferenceClickListener, /*DirectoryChooserFragment.OnFragmentInteractionListener,*/ PermissionResultListener {

    private static final String TAG = "LogPreferenceFragment";

    private final int REQUEST_CODE_WRITE_STORAGE_PERMISSION = 1;

    private SwitchPreference switchStoragePreference;
    private Preference saveLocationPreference;
//    private DirectoryChooserFragment mDialog;

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

        Toolbar toolbar = Objects.requireNonNull(getActivity()).findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.log_title));

        checkStoragePermission();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        saveLocationPreference.setSummary(pref.getString(getString(R.string.log_directory_key), android.os.Environment.getExternalStorageDirectory().getAbsolutePath()));
        setSaveLocationEnable(switchStoragePreference.isChecked());
    }

    private void checkStoragePermission() {
        int permission = PermissionChecker.checkSelfPermission(Objects.requireNonNull(getActivity()), Manifest.permission.WRITE_EXTERNAL_STORAGE);
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
                int permissionCheck = PermissionChecker.checkSelfPermission(Objects.requireNonNull(getActivity()),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    requestStoragePermission();
                }
            }
        } else if (key.equals(getString(R.string.log_directory_key))) {
            Log.d(TAG, android.os.Environment.getExternalStorageDirectory().getAbsolutePath());
            // TODO Add directory choose library
//            final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
//                    .initialDirectory(android.os.Environment.getExternalStorageDirectory().getAbsolutePath())
//                    .newDirectoryName("New Directory")
//                    .allowNewDirectoryNameModification(true)
//                    .build();
//
//            mDialog = DirectoryChooserFragment.newInstance(config);
//            mDialog.setTargetFragment(this, 0);
//            mDialog.show(getFragmentManager(), null);
        }

        return false;
    }

    private void requestStoragePermission() {
        Log.d(TAG, "requestStoragePermission");

        Activity activity = getActivity();
        if (activity instanceof SettingsActivity) {
            SettingsActivity settingsActivity = (SettingsActivity) activity;
            ActivityCompat.requestPermissions(settingsActivity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_STORAGE_PERMISSION);
            settingsActivity.setPermissionResultListener(this);
        }
    }

    public void onPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onPermissionResult");
        if (requestCode == REQUEST_CODE_WRITE_STORAGE_PERMISSION) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: GRANTED");
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: DENIED");
                    switchStoragePreference.setChecked(false);

                    boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(Objects.requireNonNull(getActivity()), permissions[i]);
                    if (!showRationale) {
                        // User choose "Don't ask again"
                        new AlertDialog.Builder(getActivity())
                                .setMessage(getString(R.string.permission_storage_message))
                                .setPositiveButton(android.R.string.ok, null)
                                .setNeutralButton(R.string.permission_storage_open_settings,
                                        (dialogInterface, i1) -> openSettings())
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
        } else {
            Log.e(TAG, "Unknown request: " + requestCode);
        }
    }

    private void openSettings() {
        Log.d(TAG, "openSettings");
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", Objects.requireNonNull(getActivity()).getPackageName(), null);
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

    // TODO Add directory choose library
//    @Override
//    public void onSelectDirectory(@NonNull String path) {
//        mDialog.dismiss();
//
//        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
//        SharedPreferences.Editor editor = pref.edit();
//        editor.putString(getString(R.string.log_directory_key), path);
//        editor.apply();
//
//        saveLocationPreference.setSummary(path);
//    }
//
//    @Override
//    public void onCancelChooser() {
//        mDialog.dismiss();
//    }
}
