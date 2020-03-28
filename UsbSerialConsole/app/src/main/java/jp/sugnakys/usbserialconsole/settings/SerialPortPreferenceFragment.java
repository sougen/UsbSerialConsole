package jp.sugnakys.usbserialconsole.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import jp.sugnakys.usbserialconsole.R;
import jp.sugnakys.usbserialconsole.UsbService;
import jp.sugnakys.usbserialconsole.util.Log;

public class SerialPortPreferenceFragment extends BasePreferenceFragment {

    private static final String TAG = "SerialPortPreferenceFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fragment_serial_port_preference);

        listPrefKeys = new String[]{
                getString(R.string.baudrate_key), getString(R.string.databits_key),
                getString(R.string.parity_key), getString(R.string.stopbits_key),
                getString(R.string.flowcontrol_key)};
    }

    @Override
    public void onResume() {
        super.onResume();

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.serial_port_title));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        super.onSharedPreferenceChanged(sharedPreferences, key);

        Log.d(TAG, "Restart UsbService");
        Intent intent = new Intent(UsbService.ACTION_SERIAL_CONFIG_CHANGED);
        getActivity().sendBroadcast(intent);
    }
}
