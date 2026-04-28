package com.monitor.health.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import com.monitor.health.Constant;

import java.util.List;

public class WifiConfigurationService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        configureWifi();
        return START_STICKY;
    }

    private void configureWifi() {
        @SuppressLint("WifiManagerLeak") WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        // Create a new WifiConfiguration
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + Constant.NETWORK_SSID + "\"";
        conf.preSharedKey = "\"" + Constant.NETWORK_PASS + "\"";
        conf.status = WifiConfiguration.Status.ENABLED;

        // Add or update the network
        int networkId = wifiManager.addNetwork(conf);
        if (networkId == -1) {
            // Network already exists, find its ID
            List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
            if (configuredNetworks != null) {
                for (WifiConfiguration existingConfig : configuredNetworks) {
                    if (existingConfig.SSID != null && existingConfig.SSID.equals(conf.SSID)) {
                        networkId = existingConfig.networkId;
                        break;
                    }
                }
            }
        }

        // Enable and connect to the network
        if (networkId != -1) {
            // Disconnect from current network
            wifiManager.disconnect();

            // Enable the chosen network
            boolean enableNetwork = wifiManager.enableNetwork(networkId, true);
            if (enableNetwork) {
                // Reconnect to the chosen network
                boolean reconnect = wifiManager.reconnect();
                if (reconnect) {
                    Log.d("Wifi", "Connecting to network: " + Constant.NETWORK_SSID);
                } else {
                    Log.d("Wifi", "Failed to reconnect to network: " + Constant.NETWORK_SSID);
                }
            } else {
                Log.d("Wifi", "Failed to enable network: " + Constant.NETWORK_SSID);
            }
        } else {
            Log.d("Wifi", "Failed to add or retrieve network configuration");
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
