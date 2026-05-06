package com.monitor.health;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class NetworkUtils {

    public enum ConnectionQuality { NONE, WEAK, STRONG }

    // Minimum downstream bandwidth considered "strong" (1 Mbps)
    private static final int STRONG_BANDWIDTH_KBPS = 1000;

    public static boolean isInternetConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            @SuppressWarnings("deprecation")
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }

    public static ConnectionQuality getConnectionQuality(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return ConnectionQuality.NONE;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                return ConnectionQuality.NONE;
            }

            // getLinkDownstreamBandwidthKbps() works for both WiFi and cellular (API 21+)
            // and reflects actual signal quality, not just network type
            int bwKbps = caps.getLinkDownstreamBandwidthKbps();
            if (bwKbps >= STRONG_BANDWIDTH_KBPS) return ConnectionQuality.STRONG;
            if (bwKbps > 0) return ConnectionQuality.WEAK;

            // Fallback for devices that report 0 bandwidth: check WiFi RSSI
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                WifiManager wifi = (WifiManager) context.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);
                if (wifi != null) {
                    int level = WifiManager.calculateSignalLevel(wifi.getConnectionInfo().getRssi(), 4);
                    return level >= 2 ? ConnectionQuality.STRONG : ConnectionQuality.WEAK;
                }
                return ConnectionQuality.WEAK;
            }
            // For cellular with 0 reported bandwidth, assume weak rather than strong
            return ConnectionQuality.WEAK;

        } else {
            @SuppressWarnings("deprecation")
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info == null || !info.isConnected()) return ConnectionQuality.NONE;
            if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                WifiManager wifi = (WifiManager) context.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);
                if (wifi != null) {
                    int level = WifiManager.calculateSignalLevel(wifi.getConnectionInfo().getRssi(), 4);
                    return level >= 2 ? ConnectionQuality.STRONG : ConnectionQuality.WEAK;
                }
                return ConnectionQuality.STRONG;
            }
            return ConnectionQuality.WEAK;
        }
    }

    public static void showSlowConnectionToast(Context context) {
        Toast.makeText(
                context.getApplicationContext(),
                "Slow connection detected. Searching for better signal…",
                Toast.LENGTH_LONG
        ).show();
    }

    /**
     * Runs {@code action} immediately when connection is STRONG, or waits and runs it
     * once the bandwidth improves. Shows a toast if the connection is currently slow.
     *
     * @return the registered NetworkCallback, or null if the action already ran.
     *         Call {@link ConnectivityManager#unregisterNetworkCallback} with the
     *         returned callback in onDestroyView() to avoid leaks.
     */
    public static ConnectivityManager.NetworkCallback scheduleOnStrongConnection(
            Context context, Runnable action) {
        Handler handler = new Handler(Looper.getMainLooper());

        ConnectionQuality quality = getConnectionQuality(context);
        if (quality == ConnectionQuality.STRONG) {
            action.run();
            return null;
        }

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return null;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // Pre-L: just try anyway
            if (quality != ConnectionQuality.NONE) action.run();
            return null;
        }

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build();

        ConnectivityManager.NetworkCallback[] cbRef = {null};
        cbRef[0] = new ConnectivityManager.NetworkCallback() {
            private boolean fired = false;

            @Override
            public void onCapabilitiesChanged(@NonNull Network network,
                                              @NonNull NetworkCapabilities caps) {
                if (!fired && caps.getLinkDownstreamBandwidthKbps() >= STRONG_BANDWIDTH_KBPS) {
                    fired = true;
                    safeUnregister(cm, cbRef[0]);
                    handler.post(action);
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                // Keep waiting — another network may still become available
            }
        };

        try {
            cm.registerNetworkCallback(request, cbRef[0]);
            return cbRef[0];
        } catch (Exception e) {
            if (quality != ConnectionQuality.NONE) action.run();
            return null;
        }
    }

    private static void safeUnregister(ConnectivityManager cm,
                                       ConnectivityManager.NetworkCallback cb) {
        try { cm.unregisterNetworkCallback(cb); } catch (Exception ignored) {}
    }
}
