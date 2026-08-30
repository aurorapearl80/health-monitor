package com.monitor.health.utility;
import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

import com.monitor.health.database.DatabaseClient;
import com.monitor.health.model.BleDeviceModel;

import java.util.List;

public class DeviceUtils {

    @SuppressLint("HardwareIds")
    public static String getIMEI(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * The serial that identifies this watch to the backend's serial-only doctor-watch APIs
     * (chat, BLE profile). Same resolution ProfileFragment/ProfileViewModel already use: the
     * first registered BLE device's serial, falling back to the Android device id.
     */
    public static String resolveWatchSerial(Context context) {
        List<BleDeviceModel> devices = DatabaseClient.getInstance(context)
                .getAppDatabase().bleDeviceDao().getAllBleDevices();
        if (!devices.isEmpty() && devices.get(0).getSerial() != null && !devices.get(0).getSerial().isEmpty()) {
            return devices.get(0).getSerial();
        }
        return getIMEI(context);
    }
}
