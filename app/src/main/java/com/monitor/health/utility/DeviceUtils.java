package com.monitor.health.utility;
import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

public class DeviceUtils {

    @SuppressLint("HardwareIds")
    public static String getIMEI(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
