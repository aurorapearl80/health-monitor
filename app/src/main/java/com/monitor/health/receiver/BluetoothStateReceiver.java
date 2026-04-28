package com.monitor.health.receiver;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BluetoothStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Log.d("BluetoothReceiver", "Bluetooth is OFF");
                    // TODO: Show notification or handle logic here
                    // Send broadcast to MainActivity
                    Intent i = new Intent("BLUETOOTH_STATE_CHANGED");
                    i.putExtra("state", "OFF");
                    context.sendBroadcast(i);
                    break;

                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.d("BluetoothReceiver", "Bluetooth is turning OFF");
                    break;

                case BluetoothAdapter.STATE_ON:
//                    Log.d("BluetoothReceiver", "Bluetooth is ON");
//                    Intent on = new Intent("BLUETOOTH_STATE_CHANGED");
//                    on.putExtra("state", "ON");
//                    context.sendBroadcast(on);
                    break;

                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.d("BluetoothReceiver", "Bluetooth is turning ON");
                    Log.d("BluetoothReceiver", "Bluetooth is ON");
                    Intent on = new Intent("BLUETOOTH_STATE_CHANGED");
                    on.putExtra("state", "ON");
                    context.sendBroadcast(on);
                    break;
            }
        }
    }

    // Helper method to register receiver
    public static void register(Context context, BluetoothStateReceiver receiver) {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(receiver, filter);
    }
}
