package jp.sugnakys.usbserialconsole.settings

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.developer.filepicker.model.DialogConfigs
import com.developer.filepicker.model.DialogProperties
import com.developer.filepicker.view.FilePickerDialog
import jp.sugnakys.usbserialconsole.R
import jp.sugnakys.usbserialconsole.util.Log
import java.io.File

class LogPreferenceFragment : BasePreferenceFragment(),
        Preference.OnPreferenceClickListener, PermissionResultListener {

    companion object {
        private const val TAG = "LogPreferenceFragment"
        private const val REQUEST_CODE_WRITE_STORAGE_PERMISSION = 1
    }

    private var switchStoragePreference: SwitchPreference? = null
    private var saveLocationPreference: Preference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.fragment_log_preference)
        val prefKeys = arrayOf(
                getString(R.string.log_switch_storage_key),
                getString(R.string.log_directory_key))
        for (prefKey in prefKeys) {
            findPreference<Preference>(prefKey)!!.onPreferenceClickListener = this
        }

        switchStoragePreference = findPreference(getString(R.string.log_switch_storage_key))
        saveLocationPreference = findPreference(getString(R.string.log_directory_key))
    }

    override fun onResume() {
        super.onResume()

        val toolbar = activity!!.findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.log_title)

        checkStoragePermission()

        val pref = PreferenceManager.getDefaultSharedPreferences(activity)
        saveLocationPreference?.summary = pref.getString(getString(R.string.log_directory_key),
                context?.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.absolutePath)
        setSaveLocationEnable(switchStoragePreference!!.isChecked)
    }

    private fun checkStoragePermission() {
        val permission = PermissionChecker.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        switchStoragePreference!!.isChecked = permission == PackageManager.PERMISSION_GRANTED
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val key = preference.key
        if (key == getString(R.string.log_switch_storage_key)) {
            if (switchStoragePreference!!.isChecked) {
                val permissionCheck = PermissionChecker.checkSelfPermission(activity!!,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    requestStoragePermission()
                }
            }
        } else if (key == getString(R.string.log_directory_key)) {
            Log.d(TAG, context?.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.absolutePath)

            val properties = DialogProperties().apply {
                selection_mode = DialogConfigs.SINGLE_MODE
                selection_type = DialogConfigs.DIR_SELECT
                root = File(DialogConfigs.DEFAULT_DIR)
                error_dir = File(DialogConfigs.DEFAULT_DIR)
                offset = File(DialogConfigs.DEFAULT_DIR)
                extensions = null
                show_hidden_files = false
            }

            val dialog = FilePickerDialog(this.context, properties)
            dialog.setDialogSelectionListener { files: Array<String?> ->
                val pref = PreferenceManager.getDefaultSharedPreferences(activity)
                val editor = pref.edit()
                editor.putString(getString(R.string.log_directory_key), files[0])
                editor.apply()
                saveLocationPreference!!.summary = files[0]
            }
            dialog.show()
        }
        return false
    }

    private fun requestStoragePermission() {
        Log.d(TAG, "requestStoragePermission")

        val settingsActivity: Activity? = activity
        if (settingsActivity is SettingsActivity) {
            ActivityCompat.requestPermissions(
                    settingsActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_WRITE_STORAGE_PERMISSION
            )
            settingsActivity.setPermissionResultListener(this)
        }
    }


    override fun onPermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.d(TAG, "onPermissionResult")

        if (requestCode == REQUEST_CODE_WRITE_STORAGE_PERMISSION) {
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: GRANTED")
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: DENIED")
                    switchStoragePreference!!.isChecked = false
                    val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity!!, permissions[i])
                    if (!showRationale) { // User choose "Don't ask again"
                        AlertDialog.Builder(activity!!)
                                .setMessage(getString(R.string.permission_storage_message))
                                .setPositiveButton(android.R.string.ok, null)
                                .setNeutralButton(R.string.permission_storage_open_settings
                                ) { _: DialogInterface?, _: Int -> openSettings() }
                                .create()
                                .show()
                    } else if (Manifest.permission.WRITE_EXTERNAL_STORAGE == permissions[i]) {
                        AlertDialog.Builder(activity!!)
                                .setMessage(getString(R.string.permission_storage_message))
                                .setPositiveButton(android.R.string.ok, null)
                                .create()
                                .show()
                    }
                }
            }
        } else {
            Log.e(TAG, "Unknown request: $requestCode")
        }
    }

    private fun openSettings() {
        Log.d(TAG, "openSettings")

        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity!!.packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        super.onSharedPreferenceChanged(sharedPreferences, key)
        if (key == getString(R.string.log_switch_storage_key)) {
            setSaveLocationEnable(findPreference<SwitchPreference>(key)!!.isChecked)
        }
    }

    private fun setSaveLocationEnable(isEnable: Boolean) {
        saveLocationPreference!!.isEnabled = isEnable
    }
}
