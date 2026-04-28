package com.monitor.health;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Set;

public class BluetoothCacheHelper {

    private static final String TAG = "BluetoothCacheHelper";

    public static void clearBluetoothCache(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Device does not support Bluetooth");
            return;
        }

        // Cancel ongoing device discovery
        bluetoothAdapter.cancelDiscovery();

        // Remove paired Bluetooth devices
        removePairedDevices(bluetoothAdapter);

        Log.i(TAG, "Bluetooth cache cleared successfully");
    }

    private static void removePairedDevices(BluetoothAdapter bluetoothAdapter) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice device : pairedDevices) {
            unpairDevice(bluetoothAdapter, device);
        }
    }

    private static void unpairDevice(BluetoothAdapter bluetoothAdapter, BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
            Log.i(TAG, "Unpaired device: " + device.getName());
        } catch (Exception e) {
            Log.e(TAG, "Error unpairing device " + device.getName() + ": " + e.getMessage());
        }
    }
}
