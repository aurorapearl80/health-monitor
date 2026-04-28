package com.monitor.health.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;
import androidx.recyclerview.widget.RecyclerView;

import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.NetworkUtils;
import com.monitor.health.R;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.model.BleDeviceModel;
import com.monitor.health.request.bledevice.AssignBleRequest;
import com.monitor.health.response.bledevice.DeviceResponse;
import com.monitor.health.utility.DeviceUtils;
import com.monitor.health.utility.PreferenceHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fixes:
 * 1) Checkbox toggling triggers other rows -> solved by "toggle request" pattern (checkbox never toggles itself)
 * 2) Slow tap -> DB work during scan moved to background thread
 * 3) Cancel/ignore pending connect when user disconnects quickly (optional but included)
 */
public class BleDeviceSelectionActivity extends AppCompatActivity {

    private static final String TAG = "BleDeviceSelection";

    private WearableRecyclerView recyclerView;
    private BleDeviceAdapter adapter;
    private final List<BleDevice> deviceList = new ArrayList<>();

    private String androidId;
    private String token;

    private ProgressBar progressBar;
    private TextView emptyStateText;
    private Button btnSearchDevices;

    private DatabaseClient databaseClient;

    // --- BLE scan fields ---
    private android.bluetooth.BluetoothAdapter bluetoothAdapter;
    private android.bluetooth.le.BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning = false;
    private final android.os.Handler bleHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private static final long SCAN_PERIOD_MS = 12_000;

    // Track pending connect request (one at a time)
    private Call<DeviceResponse> pendingConnectCall = null;
    private String pendingConnectSerial = null;

    private final android.bluetooth.le.ScanCallback scanCallbackSaveToDb = new android.bluetooth.le.ScanCallback() {

        @androidx.annotation.RequiresPermission(allOf = {
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT
        })
        @Override
        public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
            android.bluetooth.BluetoothDevice device = result.getDevice();
            if (device == null || device.getName() == null) return;

            String name = device.getName();
            String address = device.getAddress();

            if (name.contains("Thermometer")
                    || name.contains("My Oximeter")
                    || name.contains("JPD")
                    || name.contains("JPD Scale")
                    || name.contains("EMPECS")
                    || name.contains("PD_86B5")
                    || name.contains("JPD BPM")) {

                Log.d(TAG, "Found target device: " + name + " - " + address);

                // Use MAC as serial
                String serial = address != null ? address : name;

                // IMPORTANT: do DB work off main thread to avoid slow taps
                new Thread(() -> saveDeviceToDatabase(name, address, serial)).start();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE scan failed: " + errorCode);
            runOnUiThread(() -> {
                Toast.makeText(BleDeviceSelectionActivity.this,
                        "Scan failed: " + errorCode, Toast.LENGTH_SHORT).show();
                showProgressBar(false);
            });
            isScanning = false;
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_device_selection);

        recyclerView = findViewById(R.id.recycler_view_devices);
        progressBar = findViewById(R.id.progress_bar);
        emptyStateText = findViewById(R.id.empty_state_text);
        btnSearchDevices = findViewById(R.id.btn_search_devices);

        databaseClient = DatabaseClient.getInstance(getApplicationContext());

        WearableLinearLayoutManager layoutManager = new WearableLinearLayoutManager(this);
        layoutManager.setItemPrefetchEnabled(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setEdgeItemsCenteringEnabled(true);

        // Adapter uses "toggle request" pattern (checkbox does NOT toggle until user confirms)
        adapter = new BleDeviceAdapter(deviceList, this::onToggleRequested);
        recyclerView.setAdapter(adapter);

        token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
        androidId = DeviceUtils.getIMEI(getApplicationContext());

        // Set up search button click listener
        btnSearchDevices.setOnClickListener(v -> onSearchDevicesClicked());


        // Load devices from DB on background thread (avoid UI blocking)
        new Thread(this::loadDevicesFromDatabase).start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startBleScanAndSaveToDb();
        }

        showProgressBar(true);
        Log.d(TAG, "Activity created.");
    }

    /**
     * Handle search button click
     */
    @SuppressLint("MissingPermission")
    private void onSearchDevicesClicked() {
        if (isScanning) {
            // If already scanning, stop it
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stopBleScan();
            }
            Toast.makeText(this, "Scan stopped", Toast.LENGTH_SHORT).show();
        } else {
            // Start new scan
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Toast.makeText(this, "Searching for devices...", Toast.LENGTH_SHORT).show();
                startBleScanAndSaveToDb();
                showProgressBar(true);
            } else {
                Toast.makeText(this, "BLE scanning not supported on this device", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**
     * Load all BLE devices from local database and display in list
     */
    private void loadDevicesFromDatabase() {
        try {
            List<BleDeviceModel> dbDevices = databaseClient.getAppDatabase()
                    .bleDeviceDao().getAllBleDevices();

            runOnUiThread(() -> {
                deviceList.clear();

                if (dbDevices != null && !dbDevices.isEmpty()) {
                    for (BleDeviceModel dbDevice : dbDevices) {
                        BleDevice device = new BleDevice(
                                dbDevice.getDeviceName(),
                                dbDevice.getDeviceAddress(),
                                dbDevice.getSerial()
                        );
                        device.setDeviceId(dbDevice.getDeviceId());
                        device.setServerId(dbDevice.getServerId());
                        device.setChecked(dbDevice.isConnected());
                        deviceList.add(device);
                    }
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "Loaded " + deviceList.size() + " devices from database");
                    showProgressBar(false);
                } else {
                    Log.d(TAG, "No devices found in database");
                    // keep progress if scanning is on, otherwise show empty
                    showProgressBar(isScanning);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error loading devices from database", e);
            runOnUiThread(() -> showProgressBar(false));
        }
    }

    /**
     * Save device to database. If device already exists (by serial), don't add duplicate
     * NOTE: this runs on background thread (called from scan thread)
     */
    private void saveDeviceToDatabase(String deviceName, String deviceAddress, String deviceSerial) {
        try {
            BleDeviceModel existingDevice = databaseClient.getAppDatabase()
                    .bleDeviceDao().getBySerial(deviceSerial);

            if (existingDevice == null) {
                BleDeviceModel newDevice = new BleDeviceModel();
                newDevice.setSerial(deviceSerial);
                newDevice.setDeviceName(deviceName);
                newDevice.setDeviceAddress(deviceAddress);

                databaseClient.getAppDatabase()
                        .bleDeviceDao().insertIgnore(newDevice);

                Log.d(TAG, "Device saved to database: " + deviceName);

                runOnUiThread(() -> addDeviceToList(deviceName, deviceAddress, deviceSerial));
            } else {
                // If exists but UI doesn't have it (possible), ensure it's shown
                runOnUiThread(() -> addDeviceToList(deviceName, deviceAddress, deviceSerial));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving device to database", e);
        }
    }

    /**
     * Add device to RecyclerView list if not already present (UI thread)
     */
    private void addDeviceToList(String deviceName, String deviceAddress, String deviceSerial) {
        for (BleDevice device : deviceList) {
            if (device.getAddress() != null && device.getAddress().equals(deviceAddress)) {
                return;
            }
        }

        BleDevice bleDevice = new BleDevice(deviceName, deviceAddress, deviceSerial);
        deviceList.add(bleDevice);
        adapter.notifyItemInserted(deviceList.size() - 1);

        showProgressBar(false);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            stopBleScan();
        }

        // cancel pending connect call if activity is closing
        if (pendingConnectCall != null) {
            try { pendingConnectCall.cancel(); } catch (Exception ignore) {}
            pendingConnectCall = null;
            pendingConnectSerial = null;
        }
    }

    // ====== NEW FLOW: adapter asks to toggle, activity shows dialog, then applies state ======

    private void onToggleRequested(BleDevice device, int position, boolean targetChecked) {
        if (targetChecked) {
            showConfirmationDialog(device, position);
        } else {
            showDisconnectConfirmationDialog(device, position);
        }
    }

    private void showConfirmationDialog(BleDevice device, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ble_confirmation, null);

        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        TextView messageView = dialogView.findViewById(R.id.dialog_message);
        TextView deviceInfoView = dialogView.findViewById(R.id.dialog_device_info);
        Button noButton = dialogView.findViewById(R.id.btn_no);
        Button yesButton = dialogView.findViewById(R.id.btn_yes);

        titleView.setText("Confirm Device");
        messageView.setText("Connect to " + device.getName() + "?");
        deviceInfoView.setText(device.getAddress());

        builder.setView(dialogView).setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();

        noButton.setOnClickListener(v -> dialog.dismiss());

        yesButton.setOnClickListener(v -> {
            // Show immediate feedback (checkbox becomes checked now)
            device.setChecked(true);
            adapter.notifyItemChanged(position);

            // Send to server (async)
            sendDeviceToApi(device, position);

            dialog.dismiss();
        });
    }

    private void showDisconnectConfirmationDialog(BleDevice device, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ble_confirmation, null);

        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        TextView messageView = dialogView.findViewById(R.id.dialog_message);
        TextView deviceInfoView = dialogView.findViewById(R.id.dialog_device_info);
        Button noButton = dialogView.findViewById(R.id.btn_no);
        Button yesButton = dialogView.findViewById(R.id.btn_yes);

        titleView.setText("Disconnect Device");
        messageView.setText("Disconnect from " + device.getName() + "?");
        deviceInfoView.setText(device.getAddress());

        noButton.setText("KEEP");
        yesButton.setText("DISCONNECT");

        builder.setView(dialogView).setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();

        noButton.setOnClickListener(v -> dialog.dismiss());

        yesButton.setOnClickListener(v -> {
            // If this device is currently connecting, cancel connect call and just uncheck locally
            if (pendingConnectCall != null
                    && pendingConnectSerial != null
                    && pendingConnectSerial.equals(device.getSerial())) {
                try { pendingConnectCall.cancel(); } catch (Exception ignore) {}
                pendingConnectCall = null;
                pendingConnectSerial = null;
            }

            // Immediate UI feedback
            device.setChecked(false);
            adapter.notifyItemChanged(position);

            // Disconnect from server (async)
            disconnectDeviceFromApi(device, position);

            dialog.dismiss();
        });
    }

    // ====== API Calls ======

    private void sendDeviceToApi(BleDevice device, int position) {
        if (!NetworkUtils.isInternetConnected(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            device.setChecked(false);
            adapter.notifyItemChanged(position);
            return;
        }

        String deviceId = getDeviceIdByName(device.getName());
        if (deviceId == null) {
            Toast.makeText(this, "Device type not supported", Toast.LENGTH_SHORT).show();
            device.setChecked(false);
            adapter.notifyItemChanged(position);
            return;
        }

        Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show();

        // pending connect tracking
        pendingConnectSerial = device.getSerial();

        AssignBleRequest request = new AssignBleRequest(deviceId, device.getAddress());
        pendingConnectCall = ApiClient.getUserService(Constant.BASE_URL_BGM, token, androidId)
                .assignBleDevice(request);

        pendingConnectCall.enqueue(new Callback<DeviceResponse>() {
            @Override
            public void onResponse(Call<DeviceResponse> call, Response<DeviceResponse> response) {

                // if cancelled / different selection, ignore
                if (pendingConnectSerial == null || !pendingConnectSerial.equals(device.getSerial())) {
                    Log.w(TAG, "Ignoring connect result (cancelled or changed)");
                    return;
                }

                pendingConnectSerial = null;
                pendingConnectCall = null;

                if (response.isSuccessful() && response.body() != null) {
                    String serverId = null;
                    try {
                        if (response.body().getData() != null) {
                            serverId = response.body().getData().getId();
                        }
                    } catch (Exception ignore) {}

                    // Update DB in background (avoid UI lag)
                    final String finalServerId = serverId;
                    new Thread(() -> updateDeviceInDatabase(device.getSerial(), deviceId, finalServerId, true)).start();

                    // Save preferences
                    PreferenceHelper.getInstance(BleDeviceSelectionActivity.this)
                            .putString("selected_device_address", device.getAddress());
                    PreferenceHelper.getInstance(BleDeviceSelectionActivity.this)
                            .putString("selected_device_name", device.getName());
                    PreferenceHelper.getInstance(BleDeviceSelectionActivity.this)
                            .putString("selected_device_id", deviceId);

                    Toast.makeText(BleDeviceSelectionActivity.this,
                            "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();

                    // Keep checked
                    device.setChecked(true);
                    adapter.notifyItemChanged(position);

                } else {
                    Toast.makeText(BleDeviceSelectionActivity.this,
                            "Failed to connect device", Toast.LENGTH_SHORT).show();
                    device.setChecked(false);
                    adapter.notifyItemChanged(position);
                }
            }

            @Override
            public void onFailure(Call<DeviceResponse> call, Throwable t) {
                if (call.isCanceled()) {
                    Log.w(TAG, "Connect call cancelled");
                    return;
                }

                // if cancelled / different selection, ignore
                if (pendingConnectSerial == null || !pendingConnectSerial.equals(device.getSerial())) {
                    Log.w(TAG, "Ignoring connect failure (cancelled or changed)");
                    return;
                }

                pendingConnectSerial = null;
                pendingConnectCall = null;

                Toast.makeText(BleDeviceSelectionActivity.this,
                        "Connection failed", Toast.LENGTH_SHORT).show();
                device.setChecked(false);
                adapter.notifyItemChanged(position);
            }
        });
    }

    /**
     * Update device in database with deviceId, serverId, and connection status (background thread recommended)
     */
    private void updateDeviceInDatabase(String serial, String deviceId, String serverId, boolean isConnected) {
        try {
            databaseClient.getAppDatabase()
                    .bleDeviceDao().updateBySerial(serial, deviceId, serverId, isConnected);



            Log.d(TAG, "Updated device in database serial=" + serial + " connected=" + isConnected);
        } catch (Exception e) {
            Log.e(TAG, "Error updating device in database", e);
        }
    }

    private void disconnectDeviceFromApi(BleDevice device, int position) {
        if (!NetworkUtils.isInternetConnected(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            // revert to checked because user can't disconnect
            device.setChecked(true);
            adapter.notifyItemChanged(position);
            return;
        }

        Toast.makeText(this, "Disconnecting...", Toast.LENGTH_SHORT).show();

        // Query server ID using serial from database
        String serverId = null;
        try {
            serverId = databaseClient.getAppDatabase()
                    .bleDeviceDao().getServerIdBySerial(device.getSerial());
        } catch (Exception e) {
            Log.e(TAG, "Error querying server ID by serial", e);
        }

        Log.d(TAG, "The Server id " + serverId);
        Log.d(TAG, "The device serial " + device.getSerial());

        if (serverId == null || serverId.isEmpty()) {
            // no server id => local only
            new Thread(() -> {
                //removeDeviceFromDatabase(device.getSerial());
                updateDeviceInDatabase(device.getSerial(), null, null, false);
            }).start();

            Toast.makeText(this, "Disconnected (local)", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<Void> call = ApiClient.getUserService(Constant.BASE_URL_BGM, token, androidId)
            .deleteBleDevice(serverId);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {

                    Toast.makeText(BleDeviceSelectionActivity.this,
                            "Disconnected from " + device.getName(), Toast.LENGTH_SHORT).show();

                    // Update DB background
                    new Thread(() -> {
                        //removeDeviceFromDatabase(device.getSerial());
                        updateDeviceInDatabase(device.getSerial(), null, null, false);
                    }).start();

                    // Clear preferences
                    PreferenceHelper.getInstance(BleDeviceSelectionActivity.this)
                            .remove("selected_device_address");
                    PreferenceHelper.getInstance(BleDeviceSelectionActivity.this)
                            .remove("selected_device_name");
                    PreferenceHelper.getInstance(BleDeviceSelectionActivity.this)
                            .remove("selected_device_id");

                    // Keep unchecked
                    device.setChecked(false);
                    adapter.notifyItemChanged(position);

                } else {
                    Toast.makeText(BleDeviceSelectionActivity.this,
                            "Failed to disconnect device", Toast.LENGTH_SHORT).show();

                    // revert checked
                    device.setChecked(true);
                    adapter.notifyItemChanged(position);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(BleDeviceSelectionActivity.this,
                        "Disconnection failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();

                // revert checked
                device.setChecked(true);
                adapter.notifyItemChanged(position);
            }
        });
    }

    private void removeDeviceFromDatabase(String serial) {
        try {
            BleDeviceModel dbDevice = databaseClient.getAppDatabase()
                    .bleDeviceDao().getBySerial(serial);

            if (dbDevice != null) {
                databaseClient.getAppDatabase()
                        .bleDeviceDao().deleteById(dbDevice.getId());
                Log.d(TAG, "Device removed from database: " + serial);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing device from database", e);
        }
    }

    // ====== Device ID mapping ======

    private String getDeviceIdByName(String deviceName) {
        if (deviceName == null) return null;

        if (deviceName.contains("Thermometer") || deviceName.contains("My Thermometer")) {
            return Constant.DEVICE_TEMPERATURE;
        } else if (deviceName.contains("Pulse Oximeter") || deviceName.contains("My Oximeter")) {
            return Constant.DEVICE_OXIMETER;
        } else if (deviceName.contains("Blood Pressure") || deviceName.contains("BPM")) {
            return Constant.DEVICE_BP;
        } else if (deviceName.contains("Weight") || deviceName.contains("Scale")) {
            return Constant.DEVICE_WEIGHT;
        } else if (deviceName.contains("Glucose") || deviceName.contains("EMPECS")) {
            return Constant.DEVICE_GLUCOSE;
        } else if (deviceName.contains("ECG")) {
            return "5bb306382598931ffbd1b627";
        } else if (deviceName.contains("Activity")) {
            return "5bb306382598931ffbd1b629";
        }
        return null;
    }

    // ====== UI ======

    private void showProgressBar(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (emptyStateText != null) emptyStateText.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // ====== Models ======

    public static class BleDevice {
        private final String name;
        private final String address;
        private final String serial;
        private boolean isChecked;
        private String deviceId;
        private String serverId;

        public BleDevice(String name, String address, String serial) {
            this.name = name;
            this.address = address;
            this.serial = serial;
            this.isChecked = false;
        }

        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getSerial() { return serial; }

        public boolean isChecked() { return isChecked; }
        public void setChecked(boolean checked) { isChecked = checked; }

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

        public String getServerId() { return serverId; }
        public void setServerId(String serverId) { this.serverId = serverId; }

        @Override
        public String toString() {
            return "BleDevice{" +
                    "name='" + name + '\'' +
                    ", address='" + address + '\'' +
                    ", serial='" + serial + '\'' +
                    ", isChecked=" + isChecked +
                    ", deviceId='" + deviceId + '\'' +
                    ", serverId='" + serverId + '\'' +
                    '}';
        }
    }

    // ====== Adapter (toggle-request pattern) ======

    private static class BleDeviceAdapter extends RecyclerView.Adapter<BleDeviceAdapter.ViewHolder> {

        interface OnToggleRequestListener {
            void onToggleRequested(BleDevice device, int position, boolean targetChecked);
        }

        private final List<BleDevice> devices;
        private final OnToggleRequestListener listener;

        public BleDeviceAdapter(List<BleDevice> devices, OnToggleRequestListener listener) {
            this.devices = devices;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ble_device, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(devices.get(position), listener);
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final CheckBox checkBox;
            private final TextView deviceName;
            private final TextView deviceAddress;
            private final TextView deviceSerial;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                checkBox = itemView.findViewById(R.id.checkbox_device);
                deviceName = itemView.findViewById(R.id.text_device_name);
                deviceAddress = itemView.findViewById(R.id.text_device_address);
                deviceSerial = itemView.findViewById(R.id.text_device_serial);
            }

            void bind(BleDevice device, OnToggleRequestListener listener) {
                deviceName.setText(device.getName());
                deviceAddress.setText("MAC: " + device.getAddress());
                deviceSerial.setText("Serial: " + device.getSerial());

                // Set state safely
                checkBox.setOnCheckedChangeListener(null);
                checkBox.setChecked(device.isChecked());

                // Don't allow default toggle; request confirmation instead
                checkBox.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;

                    boolean targetChecked = !device.isChecked();

                    // revert visual immediately until confirmed
                    checkBox.setChecked(device.isChecked());

                    if (listener != null) {
                        listener.onToggleRequested(device, pos, targetChecked);
                    }
                });

                // Optional: tap row to toggle
                itemView.setOnClickListener(v -> checkBox.performClick());
            }
        }
    }

    // ====== Scan start/stop ======

    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @androidx.annotation.RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT
    })
    private void startBleScanAndSaveToDb() {
        try {
            if (isScanning) return;

            android.bluetooth.BluetoothManager bluetoothManager =
                    (android.bluetooth.BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            if (bluetoothManager == null) {
                Log.e(TAG, "BluetoothManager is null");
                return;
            }

            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Bluetooth is OFF", Toast.LENGTH_SHORT).show();
                return;
            }

            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bluetoothLeScanner == null) {
                Log.e(TAG, "BluetoothLeScanner is null");
                return;
            }

            android.bluetooth.le.ScanSettings settings =
                    new android.bluetooth.le.ScanSettings.Builder()
                            .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .build();

            List<android.bluetooth.le.ScanFilter> filters = new ArrayList<>();

            isScanning = true;
            Log.d(TAG, "Starting BLE scan...");

            bleHandler.postDelayed(() -> {
                if (isScanning) stopBleScan();
            }, SCAN_PERIOD_MS);

            bluetoothLeScanner.startScan(filters, settings, scanCallbackSaveToDb);

        } catch (SecurityException se) {
            Log.e(TAG, "Missing BLE permission: " + se.getMessage(), se);
            Toast.makeText(this, "Missing BLE permissions", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "startBleScanAndSaveToDb error", e);
        }
    }

    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    private void stopBleScan() {
        try {
            if (!isScanning) return;

            isScanning = false;

            if (bluetoothLeScanner != null) {
                bluetoothLeScanner.stopScan(scanCallbackSaveToDb);
            }

            Log.d(TAG, "BLE scan stopped.");
        } catch (SecurityException se) {
            Log.e(TAG, "stopBleScan missing permission", se);
        } catch (Exception e) {
            Log.e(TAG, "stopBleScan error", e);
        }
    }
}