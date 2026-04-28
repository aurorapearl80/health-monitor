package com.monitor.health;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;

public class NetworkUtils {

    public enum ConnectionQuality { NONE, WEAK, STRONG }

    public static boolean isInternetConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
            } else {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        }
        return false;
    }

    public static ConnectionQuality getConnectionQuality(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return ConnectionQuality.NONE;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (caps == null) return ConnectionQuality.NONE;

            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifi != null) {
                    int rssi = wifi.getConnectionInfo().getRssi();
                    int level = WifiManager.calculateSignalLevel(rssi, 4);
                    return level >= 2 ? ConnectionQuality.STRONG : ConnectionQuality.WEAK;
                }
                return ConnectionQuality.STRONG;
            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                try {
                    TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    if (tm != null) {
                        return isHighSpeedCellular(tm.getNetworkType()) ? ConnectionQuality.STRONG : ConnectionQuality.WEAK;
                    }
                } catch (SecurityException ignored) {}
                return ConnectionQuality.WEAK;
            }
        } else {
            @SuppressWarnings("deprecation")
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info == null || !info.isConnected()) return ConnectionQuality.NONE;
            if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifi != null) {
                    int level = WifiManager.calculateSignalLevel(wifi.getConnectionInfo().getRssi(), 4);
                    return level >= 2 ? ConnectionQuality.STRONG : ConnectionQuality.WEAK;
                }
                return ConnectionQuality.STRONG;
            }
            return isHighSpeedCellular(info.getSubtype()) ? ConnectionQuality.STRONG : ConnectionQuality.WEAK;
        }
        return ConnectionQuality.NONE;
    }

    @SuppressWarnings("deprecation")
    private static boolean isHighSpeedCellular(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_NR:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return true;
            default:
                return false;
        }
    }
}