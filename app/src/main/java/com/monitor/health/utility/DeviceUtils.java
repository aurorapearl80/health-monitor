package com.monitor.health.utility;
import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;

public class DeviceUtils {

    @SuppressLint({"MissingPermission", "HardwareIds"})
    public static String getIMEI(Context context) {
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted
            return "Permission not granted";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return telephonyManager.getImei(); // For Android 8+
        } else {
            return telephonyManager.getDeviceId(); // Deprecated in Android 10+
        }
    }
}
