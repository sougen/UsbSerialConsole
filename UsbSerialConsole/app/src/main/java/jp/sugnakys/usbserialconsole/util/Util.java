package jp.sugnakys.usbserialconsole.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v4.content.PermissionChecker;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jp.sugnakys.usbserialconsole.R;

public class Util {
    private static final String TAG = "Util";

    public static String getCurrentTime(String format) {
        return new SimpleDateFormat(format, Locale.US).format(new Date(System.currentTimeMillis()));
    }

    public static String getCurrentDateForFile() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date(System.currentTimeMillis()));
    }

    public static File getLogDir(Context context) {
        File file = getExternalDir(context);

        if (file == null) {
            // Internal storage
            file = getInternalDir(context);
        }

        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e(TAG, "Error: Cannot create Log directory");
            } else {
                Log.d(TAG, "Create Log directory");
            }
        }
        return file;
    }

    public static boolean isInternalDir(Context context, File file) {
        Log.d(TAG, "Target: " + file.getPath());

        File internal = getInternalDir(context);
        Log.d(TAG, "InternalDir: " + internal.getPath());

        return file.equals(internal);
    }

    public static File getInternalDir(Context context) {
        return new File(context.getExternalFilesDir(null), Constants.LOG_DIR_NAME);
    }

    public static File getExternalDir(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        File file = null;
        boolean isEnableExternalStorage = pref.getBoolean(context.getString(R.string.log_switch_storage_key), false);
        int permissionCheck = PermissionChecker.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (isEnableExternalStorage && permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // External storage
            String path = pref.getString(context.getString(R.string.log_directory_key), null);
            if (path != null) {
                file = new File(path);
            }
        }

        return file;
    }

    public static void setScreenOrientation(String screenOrientation, Activity activity) {
        if (screenOrientation.equals(
                activity.getString(R.string.screen_orientation_portrait_value))) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (screenOrientation.equals(
                activity.getString(R.string.screen_orientation_landscape_value))) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (screenOrientation.equals(
                activity.getString(R.string.screen_orientation_reverse_portrait_value))) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        } else if (screenOrientation.equals(
                activity.getString(R.string.screen_orientation_reverse_landscape_value))) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    public static String getLineFeedCd(String lineFeedCode, Context context) {
        if (lineFeedCode.equals(context.getString(R.string.line_feed_code_cr_value))) {
            Log.d(TAG, "Line feed code: CR");
            return Constants.CR;
        } else if (lineFeedCode.equals(context.getString(R.string.line_feed_code_lf_value))) {
            Log.d(TAG, "Line feed code: LF");
            return Constants.LF;
        } else {
            Log.d(TAG, "Line feed code: CRLF");
            return Constants.CR_LF;
        }
    }
}
