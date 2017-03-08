package jp.sugnakys.usbserialconsole.settings;

interface PermissionResultListener {
    void onPermissionResult(int requestCode, String[] permissions, int[] grantResults);
}
