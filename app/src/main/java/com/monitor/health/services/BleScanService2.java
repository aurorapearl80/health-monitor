package com.monitor.health.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.MainActivity;
import com.monitor.health.NetworkUtils;
import com.monitor.health.R;
import com.monitor.health.ReadingsRequest;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.model.BPJumper;
import com.monitor.health.model.BleDeviceModel;
import com.monitor.health.model.Day;
import com.monitor.health.model.Device;
import com.monitor.health.model.MainSchedule;
import com.monitor.health.model.MedicationDBValue;
import com.monitor.health.model.MedicationData;
import com.monitor.health.model.MedicationSchedule;
import com.monitor.health.model.MedicinePayload;
import com.monitor.health.model.Oximeter;
import com.monitor.health.model.Reading;
import com.monitor.health.model.ReadingValue;
import com.monitor.health.model.ScheduleValue;
import com.monitor.health.model.Temperature;
import com.monitor.health.model.WeighingScale;
import com.monitor.health.receiver.BleWatchdogReceiver;
import com.monitor.health.receiver.BluetoothStateReceiver;
import com.monitor.health.utility.DeviceUtils;
import com.monitor.health.utility.DoubleChangeDetector;
import com.monitor.health.utility.IntChangeDetector;
import com.monitor.health.utility.PreferenceHelper;
import com.monitor.health.utility.TimeConverter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BleScanService2 extends Service {

    // Add field at class level
    private boolean receiverRegistered = false;

    // Add fields:
    private BluetoothGatt bluetoothGatt;
    private static final String TAG = "MainActivity";
    private static final String CHANNEL_ID = "ble_scan_channel";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;

    private PowerManager.WakeLock wakeLock;
    private PowerManager.WakeLock bleWakeLock; // Add this

    // Preferences: used to remember last connected BLE device address so we can use a filtered scan when screen is OFF
    private static final String PREFS_NAME = "ble_scan_service";
    private static final String KEY_LAST_DEVICE_ADDRESS = "last_device_address";

    private static final String KEY_KNOWN_DEVICE_ADDRESSES = "known_device_addresses";

    private final Handler bleHandler = new Handler(Looper.getMainLooper());
    private boolean isConnecting = false;
    private String connectingAddress = null;
    private int connectRetry = 0;
    private static final int MAX_CONNECT_RETRY = 3;

    private final Map<String, BluetoothGatt> connectedGattMap = new ConcurrentHashMap<>();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final Handler syncHandler = new Handler(Looper.getMainLooper());

    // --- DB MAC cache (serial = MAC) ---
    private volatile Set<String> dbMacCache = Collections.emptySet();
    private volatile long lastDbMacCacheAt = 0L;
    private static final long DB_MAC_CACHE_TTL_MS = 15_000; // refresh every 15s (tune as needed)

    private static final long SYNC_INTERVAL = 5000; // 5 seconds



    private void saveLastDeviceAddress(String address) {
        if (address == null || address.isEmpty()) return;
        try {
            SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            sp.edit().putString(KEY_LAST_DEVICE_ADDRESS, address).apply();
        } catch (Exception ignored) {}
    }

    private String loadLastDeviceAddress() {
        try {
            SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            return sp.getString(KEY_LAST_DEVICE_ADDRESS, null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isScreenOn() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm == null) return true;
            // isInteractive() = screen on & user interacting (recommended for newer APIs)
            return pm.isInteractive();
        } catch (Exception ignored) {
            return true;
        }
    }

    private void saveKnownDeviceAddress(String address) {
        if (address == null || address.isEmpty()) return;
        try {
            SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            Set<String> set = sp.getStringSet(KEY_KNOWN_DEVICE_ADDRESSES, null);
            Set<String> copy = (set == null) ? new HashSet<>() : new HashSet<>(set);
            copy.add(address);
            sp.edit().putStringSet(KEY_KNOWN_DEVICE_ADDRESSES, copy).apply();
        } catch (Exception ignored) {}
    }

    private Set<String> loadKnownDeviceAddresses() {
        try {
            SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            Set<String> set = sp.getStringSet(KEY_KNOWN_DEVICE_ADDRESSES, null);
            return (set == null) ? new HashSet<>() : new HashSet<>(set);
        } catch (Exception ignored) {
            return new HashSet<>();
        }
    }

    @SuppressLint("MissingPermission")
    private void startScanDynamic(android.bluetooth.le.ScanSettings scanSettings) {
        if (bluetoothLeScanner == null) return;

        // Screen ON: keep previous behavior (unfiltered scan) so we can discover devices by name like before.
        if (isScreenOn()) {
            bluetoothLeScanner.startScan(null, scanSettings, scanCallback);
            return;
        }

        // Screen OFF: Android may pause *unfiltered* scans. Use at least one ScanFilter.
        List<android.bluetooth.le.ScanFilter> filters = getScreenOffScanFilters();
        bluetoothLeScanner.startScan(filters, scanSettings, scanCallback);
    }

//    private java.util.List<android.bluetooth.le.ScanFilter> getScreenOffScanFilters() {
//        java.util.List<android.bluetooth.le.ScanFilter> filters = new java.util.ArrayList<>();
//
//        // Build filters for ALL known devices (thermometer + oximeter + etc.)
//        java.util.Set<String> known = loadKnownDeviceAddresses();
//
//        // Ensure last address is included too
//        String lastAddr = loadLastDeviceAddress();
//        if (lastAddr != null && !lastAddr.isEmpty()) known.add(lastAddr);
//
//        for (String addr : known) {
//            if (addr != null && android.bluetooth.BluetoothAdapter.checkBluetoothAddress(addr)) {
//                filters.add(new android.bluetooth.le.ScanFilter.Builder()
//                        .setDeviceAddress(addr)
//                        .build());
//            }
//        }
//
//        // If still empty, fallback to UUID filters (only works if device advertises UUID)
//        if (filters.isEmpty()) {
//            filters.addAll(getScanFilters());
//        }
//
//        // If still empty, keep at least 1 ScanFilter instance (screen-off needs "filtered")
//        if (filters.isEmpty()) {
//            filters.add(new android.bluetooth.le.ScanFilter.Builder().build());
//        }
//
//        return filters;
//    }
    private List<android.bluetooth.le.ScanFilter> getScreenOffScanFilters() {
        List<android.bluetooth.le.ScanFilter> filters = new ArrayList<>();

        // Add ALL known devices (so screen-off scan can see thermometer + oximeter + scale)
//        java.util.Set<String> known = loadKnownDeviceAddresses();
//
//        String lastAddr = loadLastDeviceAddress();
//        if (lastAddr != null && !lastAddr.isEmpty()) known.add(lastAddr);
//
//        // âœ… Ensure the scale is always included
////        known.add("38:1E:C7:A7:52:83");
////        known.add("28:29:47:F3:3A:77");
////
//        for (String addr : known) {
//            if (android.bluetooth.BluetoothAdapter.checkBluetoothAddress(addr)) {
//                filters.add(new android.bluetooth.le.ScanFilter.Builder()
//                        .setDeviceAddress(addr)
//                        .build());
//            }
//        }

        // 2ï¸âƒ£ Add device NAME filters (separate filters = OR condition)
        filters.add(new android.bluetooth.le.ScanFilter.Builder()
                .setDeviceName("JPD Scale")
                .build());

        filters.add(new android.bluetooth.le.ScanFilter.Builder()
                .setDeviceName("JPD BPM")
                .build());

        filters.add(new android.bluetooth.le.ScanFilter.Builder()
                .setDeviceName("My Thermometer")
                .build());

        filters.add(new android.bluetooth.le.ScanFilter.Builder()
                .setDeviceName("My Oximeter")
                .build());

        filters.add(new android.bluetooth.le.ScanFilter.Builder()
                .setDeviceName("EMPECS-BBXK010027")
                .build());



        // If nothing, keep at least one filter (required for screen-off â€œfiltered scanâ€)
        if (filters.isEmpty()) {
            filters.add(new android.bluetooth.le.ScanFilter.Builder().build());
        }

        return filters;
    }


    private String token = "";
    String androidId;
    private BluetoothGattCharacteristic notificationCharacteristic;

    private List<BluetoothGattService> services = new ArrayList<>(); // Declaring services globally

    //Oximeter
    private IntChangeDetector intChangeDetector;
    private DoubleChangeDetector doubleChangeDetector;


    boolean process1 = false;
    boolean process2 = false;
    private String serial = "";
    private Handler handler = new Handler();
    private Handler handler2 = new Handler();
    private Handler handlerWeight = new Handler();
    private static final long SCAN_PERIOD = 10000; // 10 seconds
    private Handler handlerScanForever = new Handler();
    private boolean isScanning = false;

    // Create the binder instance
    private final IBinder binder = new LocalBinder();

    List<Device> devices = new ArrayList<>();

    private byte[] alarm_hour;

    private boolean isDataSent = false;
    //private double lastSentValue = -1;
    //private static final double TOLERANCE = 0.01; // Adjust as needed
    //private static final double TOLERANCE = 0.05;     // your existing tolerance
    private static final double RESET_WEIGHT = 0.5;   // treat <= this as "no one on scale"

    private boolean canSendSameValueAgain = true;     // becomes true after reset

    private double lastProcessedValue = Double.MIN_VALUE;

    private static final double TOLERANCE = 0.05;
    private static final long SAME_VALUE_RESEND_COOLDOWN_MS = 15_000; // 15 sec (adjust)
    private long lastSentAtMs = 0L;

    private double lastSentValue = Double.NaN;



    // --- Scan resilience ---
    private long SCAN_WINDOW_MS = 12_000;      // active scan duration
    private long SCAN_PAUSE_MS  = 2_000;       // pause between cycles
    private long NO_RESULT_RESET_MS = 20_000;  // watchdog: no results -> recycle scanner

    private long lastResultAt = 0;

    private Ringtone ringtone;

    private BluetoothStateReceiver bluetoothReceiver;

    private static final int NOTIFICATION_ID = 2;
    private static final String CHANNEL_ID_WAKEUP = "BLE_SERVICE_CHANNEL";
    private static final int BLE_WATCHDOG_REQUEST_CODE = 9001;
    private static final long BLE_WATCHDOG_INTERVAL_MS = 15 * 60 * 1000L; // 15 minutes

    //private Ringtone ringtone;
    private ToneGenerator toneGenerator;
    //private final Handler handler = new Handler(Looper.getMainLooper());




    private final Handler main = new Handler(Looper.getMainLooper());
    //private final AtomicBoolean isScanning = new AtomicBoolean(false);

    // Expose the service to bound clients
    public class LocalBinder extends Binder {
        public BleScanService2 getService() {
            return BleScanService2.this;
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("RESUME_ACTION")) {
                // Code to be executed when activity resumes
                //Log.d(TAG, "On resume..............................: ");
                restartBle();
            }
        }
    };


    DatabaseClient databaseClient;

    // OR use a repeating timer to refresh the timed lock:
    private Handler wakeLockHandler = new Handler(Looper.getMainLooper());
    private Runnable wakeLockRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.acquire(10 * 60 * 1000L); // Refresh every 9 minutes
            }
            wakeLockHandler.postDelayed(this, 9 * 60 * 1000L);
        }
    };


    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called - flags: " + flags + ", startId: " + startId);

        // Ensure wakelock and BLE scan stay alive when service is (re)started
        acquireWakeLock();
        if (!isScanning) {
            Log.d(TAG, "onStartCommand: BLE not scanning, starting scan");
            startBleScan();
        }

        // Re-schedule watchdog in case the service was restarted
        scheduleBleWatchdog();

        return START_STICKY;
    }



    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved - restarting service");

        // Restart the service
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent restartPendingIntent = PendingIntent.getService(
                getApplicationContext(), 1, restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartPendingIntent);

        super.onTaskRemoved(rootIntent);
    }

    private void logServiceStatus() {
        Log.d(TAG, "=== SERVICE STATUS ===");
        Log.d(TAG, "isScanning: " + isScanning);
        Log.d(TAG, "wakeLock held: " + (wakeLock != null && wakeLock.isHeld()));
        Log.d(TAG, "bluetoothAdapter enabled: " +
                (bluetoothAdapter != null && bluetoothAdapter.isEnabled()));
        Log.d(TAG, "bluetoothLeScanner null: " + (bluetoothLeScanner == null));
        Log.d(TAG, "=====================");
    }

    // Add this to your keep-alive



    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @SuppressLint({"ForegroundServiceType", "HardwareIds", "UnspecifiedRegisterReceiverFlag"})
    @Override
    public void onCreate() {
        super.onCreate();

//        createNotificationChannel();
//        startForeground(1, createNotification());


        createNotificationChannelWakeup();
        startForeground(NOTIFICATION_ID, createNotificationWakeup());
        acquireWakeLock();


        //androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        androidId = DeviceUtils.getIMEI(getApplicationContext());
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        databaseClient = DatabaseClient.getInstance(getApplicationContext());

        IntentFilter filterRESUME_ACTION = new IntentFilter("RESUME_ACTION");
        if (!receiverRegistered) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filterRESUME_ACTION, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(receiver, filterRESUME_ACTION);
            }
            receiverRegistered = true;
        }

        startDeviceSync();


        startBleScan();

        scheduleBleWatchdog();

        // Initialize the IntChangeDetector
        intChangeDetector = new IntChangeDetector();
        intChangeDetector.setOnIntChangeListener(new IntChangeDetector.OnIntChangeListener() {
            @Override
            public void onIntChange(int newInt, int oxygen) {
                // Handle the int change
                //Log.d(TAG, "Int changed: " + newInt);
                //Log.d(TAG, "Int changed oxygen: " + newInt);
            }

            @Override
            public void onIntStable(int stableInt, int oxygen) {
                // Handle the int being stable
                //Log.d(TAG, "Int is stable: " + stableInt);
                //Log.d(TAG, "Int is stable oxygen: " + oxygen);


                if (NetworkUtils.isInternetConnected(getApplicationContext())) {
                    //Get the latest data in local and send to the server that has a value of status 1
                    //Log.d(TAG, "Internet connection sent the data temperature");
                    //Don't forget
                    sendOximeter((double) stableInt, (double) oxygen, "oximeter");
                }
                else {
                    //Log.d(TAG, "No internet connection from BLE Services");
                    saveOximeter(stableInt, oxygen, 1, serial);
                }
                //Don't forget
                displayPage("BLOOD_OXYGEN");

//                sendOximeter((double) stableInt, (double) oxygen, "oximeter");
            }
        });
        //weighing scale
        doubleChangeDetector = new DoubleChangeDetector();
        doubleChangeDetector.setOnDoubleChangeListener(new DoubleChangeDetector.OnDoubleChangeListener() {
            @Override
            public void onDoubleChange(double newInt) {
                //Log.d(TAG, "Int changed: " + newInt);
                //Log.d(TAG, "Int changed oxygen: " + newInt);
                isDataSent = false; // Reset flag when value changes
            }

            @Override
            public void onDoubleStable(double stableInt) {

                long now = SystemClock.elapsedRealtime();

                boolean sameAsLastSent = !Double.isNaN(lastSentValue)
                        && Math.abs(stableInt - lastSentValue) < TOLERANCE;

                // Block duplicates, BUT only within the cooldown window
                if (sameAsLastSent && (now - lastSentAtMs) < SAME_VALUE_RESEND_COOLDOWN_MS) {
                    return;
                }

                displayPage("WEIGHT");

                if (NetworkUtils.isInternetConnected(getApplicationContext())) {
                    sendWeightScale(stableInt);
                } else {
                    Intent fallIntent = new Intent(Constant.ACTION_WEIGHT);
                    fallIntent.putExtra(Constant.VALUE_WEIGHT, (int) stableInt);
                    saveWeighingScale(stableInt, 1, serial);
                    sendBroadcast(fallIntent);
                }

                lastSentValue = stableInt;
                lastSentAtMs = now;
            }


        });
        handler2 = new Handler();
        handlerWeight = new Handler();

        //IntentFilter filter = new IntentFilter("RESUME_ACTION");
        //registerReceiver(receiver, filter);  // <-- remove this line


//        playNotificationSound();
//
//        saveData(89, 1, serial, 3,  "ML");
//        Intent fallIntent = new Intent(Constant.ACTION_BLOOD_GLUCOSE);
//        fallIntent.putExtra(Constant.VALUE_BLOOD_GLUCOSE, 23);
//        fallIntent.putExtra(Constant.VALUE_BLOOD_GLUCOSE_MAIL_VALUE, "ML");
//        fallIntent.putExtra(Constant.VALUE_BLOOD_GLUCOSE_UNIT_VALUE, "ML");
//        sendBroadcast(fallIntent);

        // Register Bluetooth state receiver
        bluetoothReceiver = new BluetoothStateReceiver();
        IntentFilter filterBluetoothReceiver = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filterBluetoothReceiver);

        // âœ… ADD THIS LINE - Start keep-alive monitoring
        //keepAliveHandler.postDelayed(keepAliveRunnable, 30000); // Start after 30 seconds

        requestBatteryExemptions();
    }

    private void startDeviceSync() {
        syncHandler.post(syncRunnable);
    }

    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            syncDevicesWithDatabase();
            syncHandler.postDelayed(this, SYNC_INTERVAL);
        }
    };

    // Add the keep-alive runnable
    private Handler keepAliveHandler = new Handler(Looper.getMainLooper());

    private Runnable keepAliveRunnable = new Runnable() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {


            // âœ… ADD: Check if BT hardware is actually responding
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                int bleState = bluetoothAdapter.getState();
                //Log.d(TAG, "BT State: " + bleState + " (12=ON, 10=OFF)");

                // If BT is on but not scanning, restart
                if (bleState == BluetoothAdapter.STATE_ON && !isScanning) {
                    //Log.e(TAG, "BT is ON but not scanning! Restarting scan...");
                    startScanSafe();
                }
            }

            // Check wake lock
            if (wakeLock != null && !wakeLock.isHeld()) {
                // Log.e(TAG, "WakeLock lost! Re-acquiring...");
                acquireWakeLock();
            }

            // âœ… ADD: Refresh BLE wake lock
            if (bleWakeLock != null && !bleWakeLock.isHeld()) {
                //Log.e(TAG, "BLE WakeLock lost! Re-acquiring...");
                acquireWakeLock();
            }

            keepAliveHandler.postDelayed(this, 30000); // Check every 30 seconds
        }
    };

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "BLE Scan Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void createNotificationChannelWakeup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel2 = new NotificationChannel(
                    CHANNEL_ID_WAKEUP,
                    "BLE Scan Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel2);
            }
        }
    }

    private Notification createNotificationWakeup() {
        return new NotificationCompat.Builder(this, CHANNEL_ID_WAKEUP)
                .setContentTitle("BLE Service Running")
                .setContentText("Monitoring BLE connections")
                .setSmallIcon(R.drawable.ic_blood_drop_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BLE scanning active")
                .setContentText("Scanning for Bluetooth devices...")
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .build();
    }

    //
    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "com.monitor.health:BleScanWakeLock"
            );
            wakeLock.setReferenceCounted(false);
        }

        if (!wakeLock.isHeld()) {
            wakeLock.acquire(30 * 1000L); // Hold for 30 seconds
            Log.d(TAG, "WakeLock acquired with screen wake");
        }
    }



    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "WakeLock released");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to release wake lock", e);
        }
    }

    private void requestBatteryExemptions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            String packageName = getPackageName();

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                new AlertDialog.Builder(this)
                        .setTitle("Medical Device Monitoring")
                        .setMessage("This medical monitoring app requires:\n\n" +
                                "1. Battery optimization: DISABLED\n" +
                                "2. Close after screen lock: DISABLED\n" +
                                "3. Auto-launch: ENABLED\n\n" +
                                "Please configure these settings for reliable monitoring.")
                        .setPositiveButton("Open Settings", (dialog, which) -> {
                            // Open battery optimization settings
                            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            startActivity(intent);

                            // Also try to open app-specific settings
                            new Handler().postDelayed(() -> {
                                Intent appIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                appIntent.setData(Uri.parse("package:" + packageName));
                                startActivity(appIntent);
                            }, 2000);
                        })
                        .setCancelable(false)
                        .show();
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startBleScan() {
        //Log.e(TAG, "Starting the BLE---------.");
        //Log.d(TAG, "startBleScan() called");
        startScanSafe(); // use the resilient starter below

//        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
//            Log.e(TAG, "Bluetooth is disabled or not supported.");
//            stopSelf();
//            return;
//        }
//
//        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
//        if (bluetoothLeScanner != null) {
        android.bluetooth.le.ScanSettings scanSettings = new android.bluetooth.le.ScanSettings.Builder()
                .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(android.bluetooth.le.ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setReportDelay(0)
                .build();

        startScanDynamic(scanSettings);
//            Log.d(TAG, "Started BLE scan");
//        } else {
//            Log.e(TAG, "BluetoothLeScanner is null");
//            stopSelf();
//        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void stopBleScan() {
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallback);
            //Log.d(TAG, "Stopped BLE scan");
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            lastResultAt = System.currentTimeMillis();
            refreshDbMacCacheIfNeeded
            // âœ… refresh DB cache sometimes (not every result)
            ();

            BluetoothDevice device = result.getDevice();
            if (device == null) return;

            String mac = device.getAddress();
            if (mac == null) return;

            // âœ… ONLY connect if MAC exists in DB (serial = MAC)
            if (!isMacInDb(mac)) {
                return; // not in DB => ignore completely
            }

            String name = device.getName(); // can be null
            Log.d(TAG, "DB device found: " + name + " - " + mac);

            // Grab scan data IF available (don't require it)
            ScanRecord scanRecord = result.getScanRecord();
            byte[] scanData = (scanRecord != null) ? scanRecord.getBytes() : null;

            // Optional: keep your JPD Scale advertising logic (only if scanData exists)
            if (name != null && name.contains("JPD Scale") && scanData != null) {
                serial = mac.replace(":", "");
                processWeightData(scanData);
                return;
            }

            // âœ… prevent reconnect spam (only for same target)
            String normMac = normalizeMac(mac);
            if (isConnecting && normMac != null && normMac.equalsIgnoreCase(connectingAddress)) return;

            if (isConnecting) return;
            isConnecting = true;

            // Stop scan so you don't connect multiple times
            stopScanSafe();

            // âœ… connect to the DB-matched device only
            connectToDevice(device);
        }

        @Override
        public void onScanFailed(int errorCode) {
            restartScannerWithBackoff();
            Log.e(TAG, "Scan failed: " + errorCode);
        }
    };


    private void refreshDbMacCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastDbMacCacheAt < DB_MAC_CACHE_TTL_MS) return;
        lastDbMacCacheAt = now;

        dbExecutor.execute(() -> {
            try {
                List<BleDeviceModel> dbDevices = databaseClient
                        .getAppDatabase()
                        .bleDeviceDao()
                        .getConnectedDevices(); // your query

                Set<String> macs = new HashSet<>();
                if (dbDevices != null) {
                    for (BleDeviceModel m : dbDevices) {
                        String mac = normalizeMac(m.getSerial());
                        if (mac != null && BluetoothAdapter.checkBluetoothAddress(mac)) {
                            macs.add(mac.toUpperCase(Locale.US));
                        }
                    }
                }

                dbMacCache = macs;

            } catch (Exception e) {
                Log.e(TAG, "refreshDbMacCacheIfNeeded error", e);
            }
        });
    }

    private boolean isMacInDb(String mac) {
        if (mac == null) return false;
        String norm = normalizeMac(mac);
        if (norm == null) return false;
        return dbMacCache.contains(norm.toUpperCase(Locale.US));
    }


    @SuppressLint("MissingPermission")
    public void startForeverBleScan() {
//        if (bluetoothLeScanner == null) {
//            bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
//        }
//
//        if (!isScanning) {
//            isScanning = true;
//            runScanLoop();
//        }
        startScanSafe();
    }



    @SuppressLint("MissingPermission")
    private void runScanLoop() {
        if (bluetoothLeScanner == null) return;

        Log.d(TAG, "Starting BLE scan..."+isScanning);
        android.bluetooth.le.ScanSettings scanSettings = new android.bluetooth.le.ScanSettings.Builder()
                .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(android.bluetooth.le.ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setReportDelay(0)
                .build();

        startScanDynamic(scanSettings);

        // Stop scan after SCAN_PERIOD, then restart
        handlerScanForever.postDelayed(() -> {
            Log.d(TAG, "Stopping BLE scan... forever "+isScanning);
            //bluetoothLeScanner.stopScan(scanCallback);

            handler.postDelayed(() -> {
                if (isScanning) {
                    runScanLoop(); // loop again
                }
            }, 2000); // Wait 2 seconds before next scan
        }, SCAN_PERIOD);
    }

    @SuppressLint("MissingPermission")
    public void stopForeverBleScan() {
//        if (bluetoothLeScanner != null) {
//            bluetoothLeScanner.stopScan(scanCallback);
//        }
//        isScanning = false;
//        handler.removeCallbacksAndMessages(null);
        stopScanSafe();
        handlerScanForever.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroy() {
        cancelBleWatchdog();
        // Stop keep-alive first
//        if (keepAliveHandler != null) {
//            keepAliveHandler.removeCallbacks(keepAliveRunnable);
//        }

        if (receiverRegistered) {
            try { unregisterReceiver(receiver); } catch (IllegalArgumentException ignore) {}
            receiverRegistered = false;
        }

        safeStopBleScan();
        isScanning = false;

        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (handler2 != null) handler2.removeCallbacksAndMessages(null);
        if (handlerWeight != null) handlerWeight.removeCallbacksAndMessages(null);
        if (handlerScanForever != null) handlerScanForever.removeCallbacksAndMessages(null);

        safeDisconnectAndCloseGatt();
        releaseWakeLock();

        try { stopForeground(true); } catch (Exception ignore) {}
        super.onDestroy();

        if (bluetoothReceiver != null) {
            unregisterReceiver(bluetoothReceiver);
            bluetoothReceiver = null;
        }

        handler.removeCallbacksAndMessages(null);
        if (ringtone != null && ringtone.isPlaying()) ringtone.stop();
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }

    }


    @Override
    public boolean onUnbind(Intent intent) {
        // Optional: decide whether to stop self when last client unbinds
        // stopSelf();
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder; // Not a bound service
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange status=" + status + " newState=" + newState);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                // IMPORTANT: close the broken gatt object
                try { gatt.close(); } catch (Exception ignore) {}
                bluetoothGatt = null;

                isConnecting = false;

                // Retry a few times (133 often recovers on retry without toggling BT)
                if (status == 133 && connectRetry < MAX_CONNECT_RETRY && connectingAddress != null) {
                    int delay = 700 * (connectRetry + 1); // 700ms, 1400ms, 2100ms
                    connectRetry++;

                    Log.d(TAG, "GATT 133 -> retry " + connectRetry + " in " + delay + "ms");

                    bleHandler.postDelayed(() -> {
                        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                            BluetoothDevice d = bluetoothAdapter.getRemoteDevice(connectingAddress);
                            connectToDevice(d);
                        } else {
                            startScanSafe();
                        }
                    }, delay);
                } else {
                    // Give up and go back to scanning
                    connectRetry = 0;
                    connectingAddress = null;
                    startScanSafe();
                }
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnecting = false;
                connectRetry = 0;

                // Discover services after a short delay (helps some thermometers)
                bleHandler.postDelayed(() -> {
                    try { gatt.discoverServices(); } catch (Exception ignore) {}
                }, 300);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnecting = false;

                try { gatt.close(); } catch (Exception ignore) {}
                bluetoothGatt = null;

                // Resume scanning so you can reconnect
                startScanSafe();
            }
        }


        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Services discovered successfully
                // Get the device associated with the GATT connection
                BluetoothDevice device = gatt.getDevice();

                // Get the device name
                String deviceName = device.getName();

                // Log the device name
                if (deviceName != null) {
                    Log.d(TAG, "Device Name: " + deviceName);
                    ///if(deviceName.contains("PD_86B5")) {
                    services = gatt.getServices(); // Assigning value to the global variable

                    for (BluetoothGattService gattService : gatt.getServices()) {
                        // Loop through characteristics of this service
                        for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                            // Check if this characteristic is the one you're interested in
                            if (gattCharacteristic.getUuid().equals(UUID.fromString("2F2DFFF4-2E85-649D-3545-3586428F5DA3"))) {
                                // React to the characteristic (e.g., read it, subscribe to notifications, etc.)
                                if ((gattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                                    // Read characteristic
                                    if (!"My Oximeter".equals(deviceName)) {
                                        gatt.readCharacteristic(gattCharacteristic);
                                    }
                                    // gatt.readCharacteristic(gattCharacteristic);
                                }
                                if ((gattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                    // Subscribe to notifications
                                    gatt.setCharacteristicNotification(gattCharacteristic, true);
                                    BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));

                                    if (descriptor != null) {
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                        gatt.writeDescriptor(descriptor);

                                    }
                                }
                            }
                            else {
                                if (!"My Oximeter".equals(deviceName)) {
                                    gatt.readCharacteristic(gattCharacteristic);
                                }
                                ///gatt.readCharacteristic(gattCharacteristic);
                            }
                        }
                    }
                    readNotificationValue(gatt, services);
                    for (BluetoothGattService service : services) {
                        // Traverse through characteristics of each service
                        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                        for (BluetoothGattCharacteristic characteristic : characteristics) {
                            // Read or write to the characteristic as needed
                            if (!"My Oximeter".equals(deviceName)) {
                                gatt.readCharacteristic(characteristic);
                            }
                            // gatt.readCharacteristic(characteristic);
                        }
                    }
                }

            } else {
                // Error discovering services
                Log.d(TAG, " Error discovering services");
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (gatt != null && characteristic != null && status == BluetoothGatt.GATT_SUCCESS) {
                // Characteristic read successfully
                byte[] data = characteristic.getValue();
                // Convert the byte array to a hex string
                if (data != null && data.length >= 8) {
                    int year = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);  // First 2 bytes for year
                    int month = data[2] & 0xFF;
                    int day = data[3] & 0xFF;
                    int hour = data[4] & 0xFF;
                    int minute = data[5] & 0xFF;
                    // Continue parsing remaining bytes as necessary
                }
                BluetoothDevice device = gatt.getDevice();
                String deviceName = device.getName();
                String deviceAddress = device.getAddress().replace(":", "");

                Log.d(TAG, "Device name: "+device.getName());
                if (deviceName.contains("Thermometer")) {
                    parseCharacteristicThermometer(data, deviceAddress);
                }
                if (deviceName.contains("My Oximeter")) {
                    Log.d(TAG,"My Oximeter This is the oximeter --------------------");
                    serial = deviceAddress;
                    processOximeter(data, deviceAddress);
                }
            }
        }
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] value = characteristic.getValue();
            BluetoothDevice device = gatt.getDevice();
            if (device.getName().contains("PD_86B5")) {
                parseDeviceStatus(gatt, characteristic, value);
            }
            Log.d(TAG, "Characteristic device name: "+device.getName());

            if (device != null && value != null) {
                String deviceName = device.getName();
                String deviceAddress = device.getAddress().replace(":", "");

                if (deviceName.contains("EMPECS") && value.length == 15) {
                    PreferenceHelper.getInstance(getApplicationContext()).putString("EMPECS", deviceAddress);
                    parseCharacteristicData(value, deviceAddress);
                }if (deviceName.contains("Thermometer") && value.length == 5) {
                    Log.d(TAG, "Thermometer here A" + value.length);
                    parseCharacteristicThermometer(value, deviceAddress);
                }if (deviceName.contains("JPD") && value.length == 8) {
                    Log.d(TAG, "JPD here send data " );
                    processDataBPJumper(value, deviceAddress);
                }if (deviceName.contains("My Oximeter")) {
                    Log.d(TAG, "My Oximeter -------------------------- ");
                    serial = deviceAddress;
                    processOximeter(value, deviceAddress);
                }if (deviceName.contains("JPD Scale")) {
                    //serial = deviceAddress;
                    //processOximeter(value, deviceAddress);
                }

            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            // Check if descriptor write was successful
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Descriptor write successful, notifications enabled
                Log.d(TAG, "Notifications enabled for characteristic");
            } else {
                Log.e(TAG, "Descriptor write error: " + status);
            }
        }
    };


    // Method to read notification value
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void readNotificationValue(BluetoothGatt gatt, List<BluetoothGattService> services) {
        int count = 0;
        for (BluetoothGattService service : services) {
            if (service.getUuid().toString().trim().equals(Constant.SERVICES_UUID)) {
                // Now try to find the desired characteristic within this service
                notificationCharacteristic = service.getCharacteristic(UUID.fromString(Constant.CHARACTERISTIC_UUID));
                //notificationCharacteristic = service.getCharacteristic(UUID.fromString("00002A18-0000-1000-8000-00805f9b34fb")); // Use the characteristic UUID you want to read
                if (notificationCharacteristic != null) {
                    // Enable notifications for the characteristic
                    gatt.setCharacteristicNotification(notificationCharacteristic, true);
                    // Configure the descriptor for notifications (required for some devices)
                    BluetoothGattDescriptor descriptor = notificationCharacteristic.getDescriptor(UUID.fromString(Constant.DESCRIPTION_UUID));
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            }
            //Thermometer
            if (service.getUuid().toString().trim().equals(Constant.THERMOMETER_SERVICE_UUID)) {
                // Now try to find the desired characteristic within this service
                notificationCharacteristic = service.getCharacteristic(UUID.fromString(Constant.THERMOMETER_CHARACTER_UUID));
                //notificationCharacteristic = service.getCharacteristic(UUID.fromString("00002A18-0000-1000-8000-00805f9b34fb")); // Use the characteristic UUID you want to read
                if (notificationCharacteristic != null) {
                    // Enable notifications for the characteristic
                    gatt.setCharacteristicNotification(notificationCharacteristic, true);
                    // Configure the descriptor for notifications (required for some devices)
                    BluetoothGattDescriptor descriptor = notificationCharacteristic.getDescriptor(UUID.fromString(Constant.DESCRIPTION_UUID));
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            }
            //Thermometer
            if (service.getUuid().toString().trim().equals(Constant.JPD_BPM_SERVICE_UUID)) {
                // Now try to find the desired characteristic within this service
                notificationCharacteristic = service.getCharacteristic(UUID.fromString(Constant.JPD_BPM_CHARACTER_UUID));
                //notificationCharacteristic = service.getCharacteristic(UUID.fromString("00002A18-0000-1000-8000-00805f9b34fb")); // Use the characteristic UUID you want to read
                if (notificationCharacteristic != null) {
                    // Enable notifications for the characteristic
                    gatt.setCharacteristicNotification(notificationCharacteristic, true);
                    // Configure the descriptor for notifications (required for some devices)
                    BluetoothGattDescriptor descriptor = notificationCharacteristic.getDescriptor(UUID.fromString(Constant.DESCRIPTION_UUID));
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            }
            //Oximeter
            if (service.getUuid().toString().trim().equals(Constant.PULSE_OXIMETER_SERVICE_UUID)) {
                // Now try to find the desired characteristic within this service
                notificationCharacteristic = service.getCharacteristic(UUID.fromString(Constant.PULSE_OXIMETER_CHARACTER_UUID));
                //notificationCharacteristic = service.getCharacteristic(UUID.fromString("00002A18-0000-1000-8000-00805f9b34fb")); // Use the characteristic UUID you want to read
                if (notificationCharacteristic != null) {
                    // Enable notifications for the characteristic
                    gatt.setCharacteristicNotification(notificationCharacteristic, true);
                    // Configure the descriptor for notifications (required for some devices)
                    BluetoothGattDescriptor descriptor = notificationCharacteristic.getDescriptor(UUID.fromString(Constant.DESCRIPTION_UUID));
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            }
            if (service.getUuid().toString().trim().equals(Constant.PULSE_OXIMETER_SERVICE_UUID)) {
                // Now try to find the desired characteristic within this service
                notificationCharacteristic = service.getCharacteristic(UUID.fromString(Constant.PULSE_OXIMETER_CHARACTER_UUID));
                //notificationCharacteristic = service.getCharacteristic(UUID.fromString("00002A18-0000-1000-8000-00805f9b34fb")); // Use the characteristic UUID you want to read
                if (notificationCharacteristic != null) {
                    // Enable notifications for the characteristic
                    gatt.setCharacteristicNotification(notificationCharacteristic, true);
                    // Configure the descriptor for notifications (required for some devices)
                    BluetoothGattDescriptor descriptor = notificationCharacteristic.getDescriptor(UUID.fromString(Constant.DESCRIPTION_UUID));
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            }

        }
    }

    private void processDataBPJumper(byte[] data, String serial) {
        int systolic = 0;
        int diastolic = 0;
        int pulseRate = 0;

        // int d0 = data[0] & 0x00FF; // variable not used
        // int d1 = data[1] & 0x00FF; // variable not used
        // int d2 = data[2] & 0x00FF; // variable not used
        int d3 = data[3] & 0x00FF;
        int d4 = data[4] & 0x00FF;
        int d5 = data[5] & 0x00FF;
        // int d6 = data[6] & 0x00FF;
        // int d7 = data[7] & 0x00FF;

        if (data.length == 7) { // temporary reading, still inflating/deflating
            // 0xFD,0xFD,0xFB,PressureH, PressureL,0X0D, 0x0A
            diastolic = d3 * 256 + d4;
        } else if (data.length == 8) {
            // 0xFD,0xFD,0xFC, SYS,DIA,PUL, 0X0D, 0x0A
            systolic = d3;
            diastolic = d4;
            pulseRate = d5;
        }
        // Retrieve the most recent BP Jumper
        double doubleSystolic = (double) systolic;
        double doubleDiastolic = (double) diastolic;
        double doubleBmp = (double) pulseRate;

        displayPage("BLOOD_PRESSURE");
        if (NetworkUtils.isInternetConnected(this)) {
            //Get the latest data in local and send to the server that has a value of status 1
            Log.d(TAG, "Double systolic {doubleSystolic}");
            Log.d(TAG, "Double doubleDiastolic {doubleDiastolic}");
            Log.d(TAG, "Double doubleBmp {doubleBmp}");
            sendBPJumper(doubleSystolic, doubleDiastolic, doubleBmp, serial);
        }
        else {
            Log.d(TAG, "No internet connection from BLE Services");
            int intSystolic = (int) systolic;
            int intDiastolic = (int) diastolic;
            int intBmp = (int) doubleBmp;
            saveDataBPJumper(intSystolic, intDiastolic, intBmp, 1, serial);
        }

    }
    private void parseCharacteristicThermometer(byte[] data, String serial) {

        Log.d(TAG, "Measurement measure running thermometer: ");
        int typeCode = data[1] & 0xFF; //type code, values can be either 55, 22 or 33
        int dataCode = ((data[2] << 8) & 0xFF00) | (data[3] & 0x00FF);

        if (typeCode == 55) {
            // object temperature
        } else if (typeCode == 22) {
            // ear temperature
        } else if (typeCode == 33) {
            //forehead temperature
        } else {
            // unknown
        }

        int tempValue = dataCode & 0x7FFF;
        double tValue = tempValue / 100.0;  // default Celcius

        int unit = (data[2] >> 8) & 0x01;
        //int unit = (data[2] >> 7) & 0x01;
        if (unit == 1) { // Fahrenheit
            Log.d("unitInDevice", "Unit is Fahrenheit: " + tempValue);
            tValue = tempValue / 100.0;
            tValue = (tValue - 32) / 1.8; // Fahrenheit
        } else {
            Log.d("unitInDevice", "Unit is Celcius");
        }
        //Don't forget
        displayPage("TEMPERATURE");

        Log.d("value", "Temperature: " + tValue);
        if (NetworkUtils.isInternetConnected(this)) {
            //Get the latest data in local and send to the server that has a value of status 1
            Log.d(TAG, "Internet connection sent the data temperature");
            //Don't forget
            sendTemperature(tValue, serial);
        }
        else {
            Log.d(TAG, "No internet connection from BLE Services");
            double formattedWeight = tempValue / 100.0f;
            saveTemperatureData(formattedWeight, 1, serial);
        }

    }
    public void sendTemperature(double temperature, String serial) {
        Log.d(TAG, "sending temperature "+ temperature);
        // Usage example in your activity or service
        token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
        Reading reading = new Reading(
                false,
                "Asia/Manila",
                "jtm00025b94050c",
                Arrays.asList(temperature),
                "5bc3cb14cba82b066cae7bc1",
                "5bb306382598931ffbd1b628",
                getDate(),
                "5bc3cb14cba82b066cae7bc1"
        );
        List<Reading> readingsList = Arrays.asList(reading);
        ReadingsRequest readingsRequest = new ReadingsRequest(readingsList);

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,token, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {

                if (response.isSuccessful()) {
                    // Request successful
                    // Handle response if needed
                    //saveTemperatureData(temperature);


                    playNotificationSound();
                    saveTemperatureData((double)temperature, 1, serial);
                    Intent fallIntent = new Intent(Constant.ACTION_TEMPERATURE);
                    fallIntent.putExtra(Constant.VALUE_WEIGHT, (double) temperature);
                    sendBroadcast(fallIntent);
                    restartBle();

                } else {
                    // Request failed
                    // Handle error
                    restartBle();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                // Request failed
                // Handle failure
                restartBle();
            }
        });
    }

    public void sendBPJumper(double systolic, double diastolic, double bpm, String serial) {
        Log.d(TAG, "Success : sending the BP");
        // Usage example in your activity or service
        //token = "\"bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%\"";
        //token = getPref("token");
        token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
        Log.d(TAG, "token this is the token "+token);
//        Reading reading = new Reading(
//                false,
//                "Asia/Manila",
//                "5be9a0e03d320b73e5f7aa71",
//                Arrays.asList(systolic,diastolic, bpm),
//                "62e42fce170f8985e63754bb",
//                "5bb306382598931ffbd1b624",
//                getDate(),
//                serial
//        );
        Reading reading = new Reading(
                false,
                "Asia/Manila",
                "jtm00025b94050c",
                Arrays.asList(systolic, diastolic, bpm),
                "66437be266c8833a1c42d7aa",
                "5bb306382598931ffbd1b624",
                getDate(),
                serial
        );
        List<Reading> readingsList = Arrays.asList(reading);
        ReadingsRequest readingsRequest = new ReadingsRequest(readingsList);

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,token, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "Success : "+response);
                Log.d(TAG, "Success : "+response.code());
                Log.d(TAG, "Success : "+response.message());
                Log.d(TAG, "Success : "+response.toString());

                playNotificationSound();
                ArrayList<Double> bloodpressureList = new ArrayList<>(Arrays.asList(systolic,diastolic, bpm));
                Intent fallIntent = new Intent(Constant.ACTION_BLOOD_PRESSURE);
                fallIntent.putExtra(Constant.VALUE_BLOOD_PRESSURE, bloodpressureList);
                sendBroadcast(fallIntent);
                Log.d("BP--- saving DB", "Systolic: " + (int)systolic + " Diastolic: " + (int)diastolic +"Blood pressure "+(int)bpm);
                saveDataBPJumper((int)systolic, (int)diastolic, (int)bpm, 1, serial);

                if (response.isSuccessful()) {
                    restartBle();
                } else {
                    restartBle();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                // Request failed
                // Handle failure
                restartBle();
            }
        });
    }

    public String getDate() {
        // Get the current date and time
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

        // Set the desired time zone offset (e.g., +08:00)
        TimeZone timeZone = TimeZone.getTimeZone("GMT+08:00");
        calendar.setTimeZone(timeZone);

        // Format the date and time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(timeZone);
        String formattedDate = sdf.format(currentDate);

        // Get the offset in hours and minutes
        int offsetMillis = timeZone.getOffset(currentDate.getTime());
        int offsetHours = offsetMillis / (60 * 60 * 1000);
        int offsetMinutes = Math.abs((offsetMillis / (60 * 1000)) % 60);

        // Format the offset
        String offset = String.format("%s%02d:%02d", offsetHours >= 0 ? "+" : "-", Math.abs(offsetHours), offsetMinutes);


        // Combine formatted date and offset
        String finalFormattedDate = formattedDate +""+offset;
        Log.d(TAG, "Date - "+offset);
        Log.d(TAG, "Date  -"+finalFormattedDate);

        return finalFormattedDate;

    }

    private void processOximeter(byte[] data, String serial) {
        // Convert unsigned bytes to integers
        int packet = data[0] & 0x81;
        int pulseRate = data[1] & 0xff;
        int spo2 = data[2] & 0xff;

        if(packet != 0x81) {
            return;
        }
        if (data.length == 4 && pulseRate > 0 && pulseRate <= 100 && spo2 > 0 && spo2 <= 100) {
            Log.d(TAG, "sendOximeter Oximeter " + pulseRate + " " + spo2);
            handler2.postDelayed(() -> intChangeDetector.updateInt(pulseRate, spo2), 500);
        } else {
            Log.w(TAG, "Invalid oximeter data: pulseRate=" + pulseRate + ", spo2=" + spo2);
        }

        //int oxygen = packet;




    }

    public void sendOximeter(double pulseRate, double oxygen, String serial2) {
        // Usage example in your activity or service
        Log.d(TAG, "Eximeter serial: "+serial);
        token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
        Reading reading = new Reading(
                false,
                "Asia/Manila",
                serial,
                Arrays.asList(oxygen, pulseRate),
                "5bc3cb14cba82b066cae7bc2",
                "5bb306382598931ffbd1b626",
                getDate(),
                serial
        );
        List<Reading> readingsList = Arrays.asList(reading);
        ReadingsRequest readingsRequest = new ReadingsRequest(readingsList);

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,token, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "sendOximeter main from "+ oxygen +" "+pulseRate);

                if (response.isSuccessful()) {
                    // Request successful
                    // Handle response if needed
                    //saveTemperatureData(temperature);
                    saveOximeter((int) pulseRate, (int) oxygen, 1, serial);

                    Intent fallIntent = new Intent(Constant.ACTION_PULSE_OXIMETER);
                    fallIntent.putExtra(Constant.VALUE_PULSE_OXIMETER_PULSE_RATE, (int)oxygen);
                    fallIntent.putExtra(Constant.VALUE_OXIMETER_PULSE_OXYGEN, (int)pulseRate);
                    sendBroadcast(fallIntent);
                    restartBle();
                    playNotificationSound();

                } else {
                    // Request failed
                    // Handle error
                    restartBle();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                // Request failed
                // Handle failure
                restartBle();
            }
        });
    }

    public void saveDataBPJumper(int systolic, int diastolic, int pulseRate, int status, String serial){
        Log.d(TAG, "Saving Data");
        BPJumper bpJumper = new BPJumper(systolic, diastolic, pulseRate, status, serial);
        databaseClient.getAppDatabase().bpJumperDao().insertBPJumper(bpJumper);
    }
    public void saveTemperatureData(double temp, int status, String serial){
        Temperature temperature = new Temperature(temp, status, serial);
        databaseClient.getAppDatabase().temperatureDao().insertTemperature(temperature);
    }

    public void saveOximeter(int pulseRate, int oxygen, int status, String serial){
        Oximeter oximeter = new Oximeter(pulseRate, oxygen, status, serial);
        databaseClient.getAppDatabase().oximeterDao().insertOximeter(oximeter);
    }

    private void playNotificationSound() {
        try {
            // Try actual default first; fall back to Settings default if needed
            Uri uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION);
            if (uri == null) {
                uri = Settings.System.DEFAULT_NOTIFICATION_URI;
            }

            ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);
            if (ringtone == null) {
                // Last-resort fallback beep if device has no notification sound
                new ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
                        .startTone(ToneGenerator.TONE_PROP_BEEP, 200);
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.setAudioAttributes(
                        new android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                );
            }

            // Optional: avoid overlapping plays
            if (ringtone.isPlaying()) ringtone.stop();
            ringtone.play();

            // Optionally stop it after a short time (some OEM tones loop)
            new Handler(getMainLooper()).postDelayed(() -> {
                try { if (ringtone != null && ringtone.isPlaying()) ringtone.stop(); } catch (Exception ignored) {}
            }, 2000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleBleWatchdog() {
        try {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am == null) return;

            Intent i = new Intent(this, BleWatchdogReceiver.class);
            i.setAction(BleWatchdogReceiver.ACTION_BLE_WATCHDOG);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pi = PendingIntent.getBroadcast(
                    this,
                    BLE_WATCHDOG_REQUEST_CODE,
                    i,
                    flags
            );

            long triggerAt = SystemClock.elapsedRealtime() + BLE_WATCHDOG_INTERVAL_MS;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pi
                );
            } else {
                am.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pi
                );
            }

            Log.d(TAG, "BLE watchdog scheduled in " + BLE_WATCHDOG_INTERVAL_MS + " ms");
        } catch (Exception e) {
            Log.e(TAG, "scheduleBleWatchdog: error", e);
        }
    }

    private void cancelBleWatchdog() {
        try {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am == null) return;

            Intent i = new Intent(this, BleWatchdogReceiver.class);
            i.setAction(BleWatchdogReceiver.ACTION_BLE_WATCHDOG);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pi = PendingIntent.getBroadcast(
                    this,
                    BLE_WATCHDOG_REQUEST_CODE,
                    i,
                    flags
            );

            am.cancel(pi);
            Log.d(TAG, "BLE watchdog cancelled");
        } catch (Exception e) {
            Log.e(TAG, "cancelBleWatchdog: error", e);
        }
    }

    public void restartBle() {
        // stopScan();
        //bluetoothAdapter.disable(); // Disable Bluetooth
//        Log.d("SecondRunnable", "Second postDelayed triggered restartBle.");
//
//        handler.postDelayed(() -> {
//            //bluetoothAdapter.enable(); // Enable Bluetooth
//
//            waitForBluetoothOnThen(() -> {
//                stopBleScan(); // Safe to call now
        //startBleScan(); // (optional) Start scanning again
//            });
//
//        }, SCAN_PERIOD);
        Log.d(TAG, "restartBle()");
        // stop safely, clear callbacks, then restart
        try { stopScanSafe(); } catch (Exception ignore) {}
        handlerScanForever.removeCallbacksAndMessages(null);
        restartScannerWithBackoff();
    }

    private void waitForBluetoothOnThen(Runnable onBluetoothOn) {
        final Handler waitHandler = new Handler();
        waitHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                    onBluetoothOn.run();
                } else {
                    waitHandler.postDelayed(this, 500); // check again after 500ms
                }
            }
        }, 500);
    }

    // Method to connect to the device and read characteristics
//    @SuppressLint("MissingPermission")
//    private void connectToDevice(BluetoothDevice device) {
//        saveLastDeviceAddress(device != null ? device.getAddress() : null);
//        Log.d(TAG, "connect me please");
//        //bluetoothLeScanner.stopScan(scanCallback); // Stop scanning
//        //bluetoothGatt = device.connectGatt(getApplicationContext(), false, gattC
//        bluetoothGatt = device.connectGatt(getApplicationContext(), false, gattCallback);
//    }
    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        if (device == null) return;

        final String addr = device.getAddress();
        if (isConnecting && addr != null && addr.equals(connectingAddress)) return;

        Log.d(TAG, "connectToDevice(): " + device.getName() + " " + addr);

        // Stop scan BEFORE connecting (reduces 133 a lot)
        stopScanSafe();

        // Close any previous GATT to avoid stale connections
        safeDisconnectAndCloseGatt();

        isConnecting = true;
        connectingAddress = addr;

        // Small delay helps many devices (esp. watches) avoid 133
        bleHandler.postDelayed(() -> {
            try {
                if (!hasBtConnectPerm()) return;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    bluetoothGatt = device.connectGatt(
                            getApplicationContext(),
                            false,
                            gattCallback,
                            BluetoothDevice.TRANSPORT_LE
                    );
                } else {
                    bluetoothGatt = device.connectGatt(getApplicationContext(), false, gattCallback);
                }
            } catch (Exception e) {
                Log.e(TAG, "connectGatt exception", e);
                isConnecting = false;
                connectingAddress = null;
                startScanSafe();
            }
        }, 300);
    }



    // Example method to process weight data
    private void processWeightData(byte[] scanRecord) {
        if (scanRecord.length >= 6) { // Ensure scanRecord is at least 6 bytes long
            byte highByte = scanRecord[4];
            byte lowByte = scanRecord[5];
            // Process weight data
            int weightRaw = ((highByte & 0xFF) << 8) | (lowByte & 0xFF);
            double weightDivided = weightRaw / 10.0;
            handlerWeight.postDelayed(() -> doubleChangeDetector.updateDouble(weightDivided), 500);
            // Log or use the weight data

        }
    }
    public void saveWeighingScale(double weight, int status, String serial){
        WeighingScale weighingScale = new WeighingScale(weight, status, serial);
        databaseClient.getAppDatabase().weighingScaleDao().insertWeighingScale(weighingScale);
    }
    public void sendWeightScale(double weight) {
        // Usage example in your activity or service
        if(weight != 0 ) {
            Log.d(TAG, "Eximeter serial: "+serial);
            token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
            Reading reading = new Reading(
                    false,
                    "Asia/Manila",
                    serial,
                    Arrays.asList(weight),
                    "5d2cac72ed5d7122d4044f0f",
                    "5bb306382598931ffbd1b625",
                    getDate(),
                    serial
            );
            List<Reading> readingsList = Arrays.asList(reading);
            ReadingsRequest readingsRequest = new ReadingsRequest(readingsList);

            ///playNotificationSound();
            Intent fallIntent = new Intent(Constant.ACTION_WEIGHT);
            fallIntent.putExtra(Constant.VALUE_WEIGHT, weight);
            sendBroadcast(fallIntent);
            saveWeighingScale(weight, 1, serial);
            restartBle();

            Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,token, androidId).sendReadings(readingsRequest);
            call.enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    Log.d(TAG, "this is the result "+ response.code());
                    Log.d(TAG, "this is the result "+ response.body());
                    Log.d(TAG, "this is the result "+ response.toString());
                    Log.d(TAG, "this is the result "+ response.message());
                    if (response.isSuccessful()) {
                        // Request successful
                        // Handle response if needed
                        //saveTemperatureData(temperature);D
                        playNotificationSound();
                        Intent fallIntent = new Intent(Constant.ACTION_WEIGHT);
                        fallIntent.putExtra(Constant.VALUE_WEIGHT, weight);
                        sendBroadcast(fallIntent);
                        saveWeighingScale(weight, 1, serial);
                        restartBle();

                    } else {
                        // Request failed
                        // Handle error
                        restartBle();
                    }
                }

                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    // Request failed
                    // Handle failure
                    restartBle();
                }
            });
        }
    }

    private void parseCharacteristicData(byte[] data, String serial) {

        // Implement your data parsing logic here
        // Example: Parse glucose level, date/time, etc.

        byte flags = data[0];
        byte yearLow = data[3];
        byte yearHigh = data[4];
        byte monthByte = data[5];
        byte dayByte = data[6];
        byte hourByte = data[7];
        byte minuteByte = data[8];
        byte secondByte = data[9];
        byte glLow = data[12];
        byte glHigh = data[13];
        byte typeLocation = data[14];

        // Process flags
        boolean glPresent = ((flags >> 1) & 0x01) == 1;
        boolean isUnit_molpL = ((flags >> 2) & 0x01) == 1;
        boolean isBeforeMeal = ((flags >> 6) & 0x01) == 1;
        boolean hasNoMealSelection = ((flags >> 6) & 0x03) == 0;
        boolean isControlSolution = (typeLocation & 0xFF) == 164;

        // Determine mealValue based on flags and typeLocation
        int mealValue;
        if (isControlSolution)
            mealValue = 5;
        else if (hasNoMealSelection)
            mealValue = 0;
        else if (isBeforeMeal)
            mealValue = 4;
        else
            mealValue = 3;

        // Check if glucose value is present
//        if (!glPresent) {
//            Log.d(TAG, "Measurement Glucose value not present");
//            return;
//        }

        int glValue;
        String unitValue;

        // Extract mantissa and exponent from glucose bytes
        int mantissa = ((glHigh & 0x0F) << 8) | (glLow & 0xFF);
        int exponent = (glHigh & 0xF0) >> 4;

        // Calculate glucose concentration based on unit type
        if (isUnit_molpL) {
            // Convert to mmol/L
            double mmolpL = mantissa * Math.pow(10, (exponent - 13));
            glValue = (int) Math.round(18 * mmolpL); // Convert to mg/dL
            unitValue = "mg/dL";
        } else {
            // Convert directly to mg/dL
            glValue = (int) Math.round(mantissa * Math.pow(10, (exponent - 11)));
            unitValue = "mg/dL";
        }
        sendData(glValue, serial, mealValue, unitValue);

    }

    public void sendData(int glucose, String serial, int mailValue,  String unitValue) {
        displayPage("BLOOD_GLUCOSE");
        // Retrieve the most recent sent measurement
        if (NetworkUtils.isInternetConnected(this)) {
            //Get the latest data in local and send to the server that has a value of status 1
            sendGlucose(glucose,serial, mailValue, unitValue);
        }
        else {
            Log.d(TAG, "No internet connection from BLE Services");
            saveData(glucose, 1, serial, mailValue,  unitValue);
            Intent fallIntent = new Intent(Constant.ACTION_BLOOD_GLUCOSE);
            fallIntent.putExtra(Constant.ACTION_BLOOD_GLUCOSE, (double)glucose);
            fallIntent.putExtra(Constant.VALUE_BLOOD_GLUCOSE_MAIL_VALUE, mailValue);
            fallIntent.putExtra(Constant.VALUE_BLOOD_GLUCOSE_UNIT_VALUE, unitValue);
            sendBroadcast(fallIntent);
        }

    }

    public void saveData(int glucose, int status, String serial, int mailValue,  String unitValue){
        Log.d(TAG, "Saving Data --- ");
        ReadingValue readingValue = new ReadingValue();
        readingValue.setEvent(12);
        readingValue.setEventDescription("This is the parentDescription");
        readingValue.setGlucose(glucose);
        readingValue.setStatus(1);
        readingValue.setSerial(serial);
        readingValue.setMailValue(mailValue);
        readingValue.setUnitValue(unitValue);
        databaseClient.getAppDatabase().readingValueDao().insertReadingValue(readingValue);

        List<ReadingValue> list = databaseClient.getAppDatabase().readingValueDao().getAllReadingValues();
        Log.d(TAG, "Reading list count : "+list.size());
    }

    public void sendGlucose(int glucose, String serial, int mailValue,  String unitValue) {
        // Usage example in your activity or service

        Log.d(TAG, "The data of glucose Reading list count : "+glucose);
        token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
        Reading reading = new Reading(
                false,
                "Asia/Manila",
                "5be9a0e03d320b73e5f7aa71",
                Arrays.asList((double)glucose, (double)mailValue),
                "5e4c0db6bc20236a64ca3467",
                "5bb306382598931ffbd1b623",
                getDate(),
                serial
        );
        List<Reading> readingsList = Arrays.asList(reading);
        ReadingsRequest readingsRequest = new ReadingsRequest(readingsList);

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,token, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {

                if (response.isSuccessful()) {
                    // Request successful
                    // Handle response if needed
                    //saveData(glucose);
                    restartBle();
                    saveData(glucose, 1, serial, mailValue, unitValue);
                    playNotificationSound();
                    Intent fallIntent = new Intent(Constant.ACTION_BLOOD_GLUCOSE);
                    fallIntent.putExtra(Constant.VALUE_BLOOD_GLUCOSE, glucose);
                    fallIntent.putExtra(Constant.VALUE_BLOOD_GLUCOSE_MAIL_VALUE, mailValue);
                    fallIntent.putExtra(Constant.VALUE_BLOOD_GLUCOSE_UNIT_VALUE, unitValue);
                    sendBroadcast(fallIntent);

                } else {
                    // Request failed
                    // Handle error
                    restartBle();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                // Request failed
                // Handle failure
                restartBle();
            }
        });
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void parseDeviceStatus(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] data) {
        if (data == null) {
            Log.d("TAG", "Data is null");
            return;
        }
        bluetoothGatt = gatt;
        //check the position if 21 meaning get the alarm value
        Log.d("TAG", "Received Data: last position 9 " + data[9]);
        Log.d("TAG", "Received Data: last position 7 " + data[7]);
        Log.d("TAG", "Received Data: last " + bytesToHexString(data));
        if(data[6] == 1) {
            process1 = true;
            Log.d("TAG", "Received Data: equal data");
            Log.d("TAG", "Received Data: Original " + bytesToHexString(data));
            data[6] = (byte) 0x81;
            data[16] = (byte) getChecksumRange(data,0,15);
            Log.d("TAG", "Received Data modifiedHexString: " + getChecksumRange(data,0,15));
            sendSecondData(gatt, data);

            Log.d("TAG", "Received Data Running the dta here.....");
            try {
                // Delay for 2 seconds (2000 milliseconds)
                // Get the BluetoothDevice from BluetoothGatt
                BluetoothDevice device = gatt.getDevice();
                // Retrieve the MAC address
                //compose for value for the second write
                //String secondWrite = "bb1108c1080002d002"+crc16ModBusHex;
                byte[] secondWrite = new byte[12];
                secondWrite[0] = (byte) 0xbb;
                secondWrite[1] = (byte) 0x11;
                secondWrite[2] = (byte) 0x08;
                secondWrite[3] = (byte) 0xc4;
                secondWrite[4] = (byte) 0x08;
                secondWrite[5] = (byte) 0x00;
                secondWrite[6] = (byte) 0x02;
                secondWrite[7] = (byte) 0xd0;
                secondWrite[8] = (byte) 0x02;
                secondWrite[9] = (byte) 0xA5;
                secondWrite[10] = (byte) 0xF0;
                secondWrite[11] = (byte) getChecksumRange(secondWrite,0,10);
                Thread.sleep(3000);
                sendSecondData(gatt, secondWrite);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if((data[6] & 0x00ff) == 0x82) {
            process2 = true;
            Log.e(TAG, "Received data 0x82 ------------ ");

            // runTheAlarm(gatt);

            //IF OKAY MAKE A TIMER FOR THIS CODE
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            // Define your task as a Runnable
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    // Task logic here
                    Log.e(TAG, "Received Data: last stop watch------------ 1 hours");
                    runTheAlarm(gatt);
                }
            };
            scheduler.scheduleAtFixedRate(task, 1, 1, TimeUnit.HOURS);


        }
        //chedk the btye after reading the alarm
        //send the data to api
        if(data[7] == 33) {
            Log.e(TAG, "Received star reading the data here");
            setPreAlarmValues(data);
        }
        //for setting the alarm only
        if(data[7] == 97) {

            int firstHour = data[9];
            int firstMinute = data[12];
            String period = (firstHour >= 12) ? "PM" : "AM";
            int hourIn12HourFormat = (firstHour % 12 == 0) ? 12 : firstHour % 12;
            String formattedHour = String.format("%02d", hourIn12HourFormat);
            String formattedMinute = String.format("%02d", firstMinute);
            String formattedTime = formattedHour + ":" + formattedMinute + " " + period;
            //compare to the database
            List<MedicationDBValue> medicationDBValueList = databaseClient.getAppDatabase().medicationDBValueDao().getConfigurationParameter(data[7]);
            if(medicationDBValueList.size() != 0) {
                //if not equal set the alarm
                if(!medicationDBValueList.get(0).getTime().equals(formattedTime)) {
                    Log.e(TAG, "Received Data: last this data in db is not equal");
                    // Split the string based on the colon (":")
                    String[] timeParts = medicationDBValueList.get(0).getTimeDisplay().split(":");
                    // Convert the hour and minute to integers
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    setAlarmTime(gatt, (byte) 0x61, (byte) hour, (byte) 0x71, (byte) minute);
                }
            } else {
                //delete meaning no data in this position of alarm
                if(data[9] != -1) {
                    setAlarmTime(gatt, (byte) 0x61, (byte) 0xff, (byte) 0x71, (byte) 0xff);
                }
            }
            Log.e(TAG, "Received data this the data first 97------------ "+firstHour);
            int secondHour = data[15];
            int secondMinute = data[18];
            String secondPeriod = (secondHour >= 12) ? "PM" : "AM";
            int secondHourIn12HourFormat = (secondHour % 12 == 0) ? 12 : secondHour % 12;
            String secondFormattedHour = String.format("%02d", secondHourIn12HourFormat);
            String secondFormattedMinute = String.format("%02d", secondMinute);
            String secondFormattedTime = secondFormattedHour + ":" + secondFormattedMinute + " " + secondPeriod;
            //compare to the database
            List<MedicationDBValue> secondMedicationDBValueList = databaseClient.getAppDatabase().medicationDBValueDao().getConfigurationParameter(data[13]);
            if(secondMedicationDBValueList.size() != 0) {
                if(!secondMedicationDBValueList.get(0).getTime().equals(secondFormattedTime)) {
                    Log.e(TAG, "Received Data: last this data in db is not equal");
                    // Split the string based on the colon (":")
                    String[] timeParts = medicationDBValueList.get(0).getTimeDisplay().split(":");
                    // Convert the hour and minute to integers
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    setAlarmTime(gatt, (byte) 0x62, (byte) hour, (byte) 0x72, (byte) minute);
                }
            }
            else {
                //delete meaning no data in this position of alarm
                if(data[9] != -1) {
                    setAlarmTime(gatt, (byte) 0x62, (byte) 0xff, (byte) 0x72, (byte) 0xff);
                }
            }
            Log.e(TAG, "Received Data: last 97 ------------ "+data[7]);
            //compare to the query that you get.
        }
        if(data[7] == 99) {

            int firstHour = data[9];
            int firstMinute = data[12];
            String period = (firstHour >= 12) ? "PM" : "AM";
            int hourIn12HourFormat = (firstHour % 12 == 0) ? 12 : firstHour % 12;
            String formattedHour = String.format("%02d", hourIn12HourFormat);
            String formattedMinute = String.format("%02d", firstMinute);
            String formattedTime = formattedHour + ":" + formattedMinute + " " + period;
            //compare to the database
            List<MedicationDBValue> medicationDBValueList = databaseClient.getAppDatabase().medicationDBValueDao().getConfigurationParameter(data[7]);
            if(medicationDBValueList.size() != 0) {
                if(!medicationDBValueList.get(0).getTime().equals(formattedTime)) {
                    Log.e(TAG, "Received Data: last this data in db is not equal");
                    // Split the string based on the colon (":")
                    String[] timeParts = medicationDBValueList.get(0).getTimeDisplay().split(":");
                    // Convert the hour and minute to integers
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    setAlarmTime(gatt, (byte) 0x63, (byte) hour, (byte) 0x73, (byte) minute);
                }
            }
            else {
                //delete meaning no data in this position of alarm
                if(data[9] != -1) {
                    setAlarmTime(gatt, (byte) 0x63, (byte) 0xff, (byte) 0x73, (byte) 0xff);
                }
            }
            Log.e(TAG, "Received Data: last 99------------ "+firstHour);
            int secondHour = data[15];
            int secondMinute = data[18];
            String secondPeriod = (secondHour >= 12) ? "PM" : "AM";
            int secondHourIn12HourFormat = (secondHour % 12 == 0) ? 12 : secondHour % 12;
            String secondFormattedHour = String.format("%02d", secondHourIn12HourFormat);
            String secondFormattedMinute = String.format("%02d", secondMinute);
            String secondFormattedTime = secondFormattedHour + ":" + secondFormattedMinute + " " + secondPeriod;
            //compare to the database
            List<MedicationDBValue> secondMedicationDBValueList = databaseClient.getAppDatabase().medicationDBValueDao().getConfigurationParameter(data[13]);
            if(secondMedicationDBValueList.size() != 0) {
                if(!secondMedicationDBValueList.get(0).getTime().equals(secondFormattedTime)) {
                    Log.e(TAG, "Received Data: last this data in db is not equal");
                    // Split the string based on the colon (":")
                    String[] timeParts = medicationDBValueList.get(0).getTimeDisplay().split(":");
                    // Convert the hour and minute to integers
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    setAlarmTime(gatt, (byte) 0x64, (byte) hour, (byte) 0x74, (byte) minute);
                }
            }
            else {
                //delete meaning no data in this position of alarm
                if(data[9] != -1) {
                    setAlarmTime(gatt, (byte) 0x64, (byte) 0xff, (byte) 0x74, (byte) 0xff);
                }
            }
            Log.e(TAG, "Received Data: last------------ "+data[7]);
        }
        if(data[7] == 101) {
            int firstHour = data[9];
            int firstMinute = data[12];
            String period = (firstHour >= 12) ? "PM" : "AM";
            int hourIn12HourFormat = (firstHour % 12 == 0) ? 12 : firstHour % 12;
            String formattedHour = String.format("%02d", hourIn12HourFormat);
            String formattedMinute = String.format("%02d", firstMinute);
            String formattedTime = formattedHour + ":" + formattedMinute + " " + period;
            //compare to the database
            List<MedicationDBValue> medicationDBValueList = databaseClient.getAppDatabase().medicationDBValueDao().getConfigurationParameter(data[7]);
            if(medicationDBValueList.size() != 0) {
                if(!medicationDBValueList.get(0).getTime().equals(formattedTime)) {
                    Log.e(TAG, "Received Data: last this data in db is not equal");
                    // Split the string based on the colon (":")
                    String[] timeParts = medicationDBValueList.get(0).getTimeDisplay().split(":");
                    // Convert the hour and minute to integers
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    setAlarmTime(gatt, (byte) 0x65, (byte) hour, (byte) 0x75, (byte) minute);
                }
            } else {
                //delete meaning no data in this position of alarm
                if(data[9] != -1) {
                    setAlarmTime(gatt, (byte) 0x65, (byte) 0xff, (byte) 0x75, (byte) 0xff);
                }
            }
            Log.e(TAG, "Received Data: last 101------------ "+firstHour);
            int secondHour = data[15];
            int secondMinute = data[18];
            String secondPeriod = (secondHour >= 12) ? "PM" : "AM";
            int secondHourIn12HourFormat = (secondHour % 12 == 0) ? 12 : secondHour % 12;
            String secondFormattedHour = String.format("%02d", secondHourIn12HourFormat);
            String secondFormattedMinute = String.format("%02d", secondMinute);
            String secondFormattedTime = secondFormattedHour + ":" + secondFormattedMinute + " " + secondPeriod;
            //compare to the database
            List<MedicationDBValue> secondMedicationDBValueList = databaseClient.getAppDatabase().medicationDBValueDao().getConfigurationParameter(data[13]);
            if(secondMedicationDBValueList.size() != 0) {
                if(!secondMedicationDBValueList.get(0).getTime().equals(secondFormattedTime)) {
                    Log.e(TAG, "Received Data: last this data in db is not equal");
                    // Split the string based on the colon (":")
                    String[] timeParts = medicationDBValueList.get(0).getTimeDisplay().split(":");
                    // Convert the hour and minute to integers
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    setAlarmTime(gatt, (byte) 0x66, (byte) hour, (byte) 0x76, (byte) minute);
                }
            } else {
                //delete meaning no data in this position of alarm
                if(data[9] != -1) {
                    setAlarmTime(gatt, (byte) 0x66, (byte) 0xff, (byte) 0x76, (byte) 0xff);
                }
            }
            Log.e(TAG, "Received Data: last 101 ------------ "+data[7]);
        }
        if(data[7] == 103 && data[6] != 6) {
            int firstHour = data[9];
            int firstMinute = data[12];
            String period = (firstHour >= 12) ? "PM" : "AM";
            int hourIn12HourFormat = (firstHour % 12 == 0) ? 12 : firstHour % 12;
            String formattedHour = String.format("%02d", hourIn12HourFormat);
            String formattedMinute = String.format("%02d", firstMinute);
            String formattedTime = formattedHour + ":" + formattedMinute + " " + period;
            //compare to the database
            List<MedicationDBValue> medicationDBValueList = databaseClient.getAppDatabase().medicationDBValueDao().getConfigurationParameter(data[7]);
            if(medicationDBValueList.size() != 0) {
                if(!medicationDBValueList.get(0).getTime().equals(formattedTime)) {
                    Log.e(TAG, "Received Data: last this data in db is not equal");
                    // Split the string based on the colon (":")
                    String[] timeParts = medicationDBValueList.get(0).getTimeDisplay().split(":");
                    // Convert the hour and minute to integers
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    setAlarmTime(gatt, (byte) 0x67, (byte) hour, (byte) 0x77, (byte) minute);
                }
            } else {
                //delete meaning no data in this position of alarm
                if(data[9] != -1) {
                    setAlarmTime(gatt, (byte) 0x67, (byte) 0xff, (byte) 0x77, (byte) 0xff);
                }
            }
            Log.e(TAG, "Received Data: last 103 ni------------ "+firstHour);
            int secondHour = data[15];
            int secondMinute = data[18];
            String secondPeriod = (secondHour >= 12) ? "PM" : "AM";
            int secondHourIn12HourFormat = (secondHour % 12 == 0) ? 12 : secondHour % 12;
            String secondFormattedHour = String.format("%02d", secondHourIn12HourFormat);
            String secondFormattedMinute = String.format("%02d", secondMinute);
            String secondFormattedTime = secondFormattedHour + ":" + secondFormattedMinute + " " + secondPeriod;
            //compare to the database
            List<MedicationDBValue> secondMedicationDBValueList = databaseClient.getAppDatabase().medicationDBValueDao().getConfigurationParameter(data[13]);
            if(secondMedicationDBValueList.size() != 0) {
                if(!secondMedicationDBValueList.get(0).getTime().equals(secondFormattedTime)) {
                    Log.e(TAG, "Received Data: last this data in db is not equal");
                    // Split the string based on the colon (":")
                    String[] timeParts = medicationDBValueList.get(0).getTimeDisplay().split(":");
                    // Convert the hour and minute to integers
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    setAlarmTime(gatt, (byte) 0x68, (byte) hour, (byte) 0x78, (byte) minute);
                }
            }
            else {
                //delete meaning no data in this position of alarm
                if(data[9] != -1) {
                    setAlarmTime(gatt, (byte) 0x68, (byte) 0xff, (byte) 0x78, (byte) 0xff);
                }
            }
            Log.e(TAG, "Received Data: last 103 ------------ "+data[7]);
        }

        if(data[7] == 105) {
            int firstHour = data[9];
            int firstMinute = data[12];
            String period = (firstHour >= 12) ? "PM" : "AM";
            int hourIn12HourFormat = (firstHour % 12 == 0) ? 12 : firstHour % 12;
            String formattedHour = String.format("%02d", hourIn12HourFormat);
            String formattedMinute = String.format("%02d", firstMinute);
            String formattedTime = formattedHour + ":" + formattedMinute + " " + period;
            //compare to the database
            List<MedicationDBValue> medicationDBValueList = databaseClient.getAppDatabase().medicationDBValueDao().getConfigurationParameter(data[7]);
            if(medicationDBValueList.size() != 0) {
                if(!medicationDBValueList.get(0).getTime().equals(formattedTime)) {
                    Log.e(TAG, "Received Data: last this data in db is not equal");
                    // Split the string based on the colon (":")
                    String[] timeParts = medicationDBValueList.get(0).getTimeDisplay().split(":");
                    // Convert the hour and minute to integers
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    setAlarmTime(gatt, (byte) 0x69, (byte) hour, (byte) 0x79, (byte) minute);
                }
            } else {
                Log.e(TAG, "Received Data: last 105 delete alarm - "+ data[0]);
                //delete meaning no data in this position of alarm
                if(data[9] != -1) {
                    setAlarmTime(gatt, (byte) 0x69, (byte) 0xff, (byte) 0x79, (byte) 0xff);
                }
            }
            Log.e(TAG, "Received Data: last 105------------ "+firstHour);
        }

    }
    public void runTheAlarm(BluetoothGatt gatt) {
        Handler handler = new Handler(Looper.getMainLooper());  // Runs on the main UI thread
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Code to execute after 2 seconds
                Log.e(TAG, "Received Data: last  getListMedicineSchedules(); ");
                getListMedicineSchedules();
            }
        }, 2000);  // Delay in milliseconds

        Handler handler2 = new Handler(Looper.getMainLooper());  // Runs on the main UI thread
        handler2.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Code to execute after 2 seconds
                Log.e(TAG, "Received Data: last initializeGetAlarm(gatt) ");
                initializeGetAlarm(gatt);
            }
        }, 4000);  // Delay in millisecon
    }
    public void initializeGetAlarm(BluetoothGatt gatt) {
        //get alarm
        Handler handler = new Handler(Looper.getMainLooper());  // Runs on the main UI thread
        handler.postDelayed(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                // Code to execute after 2 seconds
                Log.e(TAG, "Received Data: last first ------------ ");
                getAlarmHours(gatt, (byte)0x61, (byte)0x71, (byte)0x62, (byte)0x72);
            }
        }, 2000);  // Delay in milliseconds

        Handler handler2 = new Handler(Looper.getMainLooper());  // Runs on the main UI thread
        handler2.postDelayed(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                // Code to execute after 2 seconds
                Log.e(TAG, "Received Data: last second ------------ ");
                //getSecondAlarmHours63to64(gatt);
                getAlarmHours(gatt, (byte)0x63, (byte)0x73, (byte)0x64, (byte)0x74);
            }
        }, 4000);  // Delay in millisecon

        Handler handler3 = new Handler(Looper.getMainLooper());  // Runs on the main UI thread
        handler3.postDelayed(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                // Code to execute after 2 seconds
                Log.e(TAG, "Received Data: last second ------------ ");
                //getSecondAlarmHours63to64(gatt);
                getAlarmHours(gatt, (byte)0x65, (byte)0x75, (byte)0x66, (byte)0x76);
            }
        }, 6000);  // Delay in millisecon// ds

        Handler handler4 = new Handler(Looper.getMainLooper());  // Runs on the main UI thread
        handler4.postDelayed(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                // Code to execute after 2 seconds
                Log.e(TAG, "Received Data: last second ------------ ");
                //getSecondAlarmHours63to64(gatt);
                getAlarmHours(gatt, (byte)0x67, (byte)0x77, (byte)0x68, (byte)0x78);
            }
        }, 8000);  // Delay in millisecon// ds
        Handler handler5 = new Handler(Looper.getMainLooper());  // Runs on the main UI thread
        handler5.postDelayed(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                // Code to execute after 2 seconds
                Log.e(TAG, "Received Data: last second ------------ ");
                //getSecondAlarmHours63to64(gatt);
                //single call only
                getAlarmHoursLastValue(gatt, (byte)0x69, (byte)0x79);
            }
        }, 10000);  // Delay in millisecon// ds
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void setAlarmTime(BluetoothGatt gatt, byte hourTag, byte hourValue, byte minuteTag, byte minuteValue) {

        byte[] alarm = new byte[14];
        alarm[0] = (byte) 0xbb;
        alarm[1] = (byte) 0x11;
        alarm[2] = (byte) 0x0a; // add 4
        alarm[3] = (byte) 0xc7;
        alarm[4] = (byte) 0x08;
        alarm[5] = (byte) 0x00;
        alarm[6] = (byte) 0x06; // is for request package type
        //first hour
        //alarm[7] = (byte) 0x61;
        alarm[7] = hourTag;
        alarm[8] = (byte) 0x01;
        alarm[9] = hourValue;
        //minutes
        //alarm[10] = (byte) 0x71;
        alarm[10] =  minuteTag;
        alarm[11] = (byte) 0x01;
        alarm[12] = minuteValue;

        alarm[13] = (byte) getChecksumRange(alarm,0,alarm.length - 2);;
        StringBuilder hexStringNewAlarm = new StringBuilder();
        //Log.d(TAG, "New Alarm Data:");
        for (byte b : alarm) {
            //Log.e(TAG, "result newAlarm " + b);
            hexStringNewAlarm.append(String.format("%02X ", b)); // Convert byte to hex and append to string
        }
        Log.d(TAG, "Received setting the alarm: setAlarmTime " + hexStringNewAlarm.toString());
        sendFirstData(gatt, alarm);
    }

    public void setPreAlarmValues(byte[] data) {
        Log.d(TAG, "Received setting the alarm: setPreAlarmValues");
        //21 Alarm1-status
        int[] alarmIndexes = {7, 10, 13, 16, 19, 22, 25, 28, 31};
        int[] alarmValues = {33, 34, 35, 36, 37, 38, 39, 40, 41}; // Alarm status values to check
        int[] statusPositions = {9, 12, 15, 18, 21, 24, 27, 30, 33}; // Positions of alarmValueStatus in the data array
        for (int i = 0; i < alarmValues.length; i++) {
            if (alarmIndexes[i] < data.length && statusPositions[i] < data.length) {

                if (data[alarmIndexes[i]] == alarmValues[i]) {
                    int alarmValueStatus = data[statusPositions[i]];
                    Log.d("TAG", "Received status of alarm " + statusPositions[i] + " " + alarmValueStatus);
                    // Get all credentials in the DB
                    List<MedicationDBValue> medicationDBValueList = databaseClient.getAppDatabase().medicationDBValueDao().getStateParameter(data[alarmIndexes[i]]);
                    Log.d("TAG", "Received Data: size db " + medicationDBValueList.size());

                    if (medicationDBValueList.size() != 0) {
                        Log.d("TAG", "Received Data: size db " + medicationDBValueList.size());
                        Log.d("TAG", "Received Data: " + medicationDBValueList.get(0).getName());
                        // Send to API after the delay
                        sendMedicineTaken(alarmValueStatus, medicationDBValueList);

                    }
                }
            }
        }
    }

    public void sendMedicineTaken(int alarmValueStatus, List<MedicationDBValue> medicationDBValueList ) {
        // Get the current time and date using Calendar
        // value 3 only
        Log.d(TAG, "DB data "+medicationDBValueList.get(0).toString());
        if(alarmValueStatus == 3 ) {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");
            String timeTakenStr = timeFormat.format(calendar.getTime());
            calendar.add(Calendar.MINUTE, 5);  // Add 5 minutes for exactTimeTaken
            String exactTimeTakenStr = timeFormat.format(calendar.getTime());
            calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String dateTakenStr = dateFormat.format(calendar.getTime());
            Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,token, androidId).saveMedicineSchedule(new MedicinePayload(
                            medicationDBValueList.get(0).getReferenceMedicineId(),
                            medicationDBValueList.get(0).getReferenceMedicineId(),
                            medicationDBValueList.get(0).getName(),
                            medicationDBValueList.get(0).getType2(),
                            medicationDBValueList.get(0).getType(),
                            medicationDBValueList.get(0).getAmount(),
                            medicationDBValueList.get(0).getUnit(),
                            timeTakenStr,
                            timeTakenStr,
                            dateTakenStr
                    )
            );
            call.enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    Log.d(TAG, "this is the saveMedicineSchedule "+ response.code());
                    Log.d(TAG, "this is the result "+ response.code());
                    Log.d(TAG, "this is the result "+ response.body());
                    Log.d(TAG, "this is the result "+ response.toString());
                    Log.d(TAG, "this is the result "+ response.message());
                }

                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    // Request failed
                    // Handle failure
                    restartBle();
                }
            });
        }
    }                                                 //0x61                 //0x71                        //0x62                        //0x72
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void getAlarmHours(BluetoothGatt gatt, byte firstAlarmTagHour, byte firstAlarmTagMinute, byte secondAlarmTagHour, byte secondAlarmTagMinute) {
        byte[] alarm = new byte[20];
        alarm[0] = (byte) 0xbb;
        alarm[1] = (byte) 0x11;
        //alarm[2] = (byte) 0x07; // add 4
//        alarm[2] = (byte) 0x0a; // add 4
        alarm[2] = (byte) 0x10; // add 4
        alarm[3] = (byte) 0xc5;
        alarm[4] = (byte) 0x08;
        alarm[5] = (byte) 0x00;
        alarm[6] = (byte) 0x05; // is for package type read device configuration
        //first hour
        alarm[7] =  firstAlarmTagHour;
        alarm[8] = (byte) 0x01;
        alarm[9] = (byte) 0x00;
        //minutes
        alarm[10] = firstAlarmTagMinute;
        alarm[11] = (byte) 0x01;
        alarm[12] = (byte) 0x00;
        //2nd hours
        alarm[13] = secondAlarmTagHour;
        alarm[14] = (byte) 0x01;
        alarm[15] = (byte) 0x00;
        //2nd minutes
        alarm[16] = secondAlarmTagMinute;
        alarm[17] = (byte) 0x01;
        alarm[18] = (byte) 0x00;
        alarm[19] = (byte) getChecksumRange(alarm,0,alarm.length - 2);
        sendFirstData(gatt, alarm);

    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void getAlarmHoursLastValue(BluetoothGatt gatt, byte firstAlarmTagHour, byte firstAlarmTagMinute) {
        byte[] alarm = new byte[14];
        alarm[0] = (byte) 0xbb;
        alarm[1] = (byte) 0x11;
        //alarm[2] = (byte) 0x07; // add 4
//        alarm[2] = (byte) 0x0a; // add 4
        alarm[2] = (byte) 0x0a; // add 4
        alarm[3] = (byte) 0xc5;
        alarm[4] = (byte) 0x08;
        alarm[5] = (byte) 0x00;
        alarm[6] = (byte) 0x05; // is for package type read device configuration
        //first hour
        alarm[7] =  firstAlarmTagHour;
        alarm[8] = (byte) 0x01;
        alarm[9] = (byte) 0x00;
        //minutes
        alarm[10] = firstAlarmTagMinute;
        alarm[11] = (byte) 0x01;
        alarm[12] = (byte) 0x00;

        alarm[13] = (byte) getChecksumRange(alarm,0,alarm.length - 2);
        sendFirstData(gatt, alarm);

    }


    @SuppressLint("MissingPermission")
    public void setAlarmTime(BluetoothGatt gatt, byte[] alarm_hour, List<MedicationDBValue> medicationDBValueList) {
        Log.d(TAG, "result alarm size: " + alarm_hour.length);

        byte[] alarm = new byte[alarm_hour.length + 7];
        alarm[0] = (byte) 0xbb;
        alarm[1] = (byte) 0x11;
        alarm[2] = (byte) 0x10; // add 4 get all total
        alarm[3] = (byte) 0xc7;
        alarm[4] = (byte) 0x08;
        alarm[5] = (byte) 0x00;
        alarm[6] = (byte) 0x06; // is for request package type

        // Append alarm_hour values to the alarm array starting at index 7
        for (int i = 0; i < alarm_hour.length; i++) {
            alarm[7 + i] = alarm_hour[i];
        }

        StringBuilder hexString = new StringBuilder();
        for (byte b : alarm) {
            hexString.append(String.format("%02X ", b)); // Convert byte to hex and append to string
        }

        // Find the index where 0x63 occurs
        int start = -1;
        for (int i = 0; i < alarm.length; i++) {
            if (alarm[i] == (byte) 0x63) {
                start = i;
                break;
            }
        }

        // If 0x63 is found, set end to the last index of the array
        if (start != -1) {
            int end = alarm.length - 1;

            byte[] removedData = new byte[end - start + 1];
            System.arraycopy(alarm, start, removedData, 0, removedData.length);
            byte[] newAlarm = new byte[start]; // This will only include the elements before `0x63`
            System.arraycopy(alarm, 0, newAlarm, 0, newAlarm.length);
            byte lastValueNewAlarm = newAlarm[newAlarm.length - 1];
            Log.d(TAG, "Last Value of newAlarm: " + String.format("%02X", lastValueNewAlarm));
            byte[] newAlarmWithLastValue = new byte[newAlarm.length + 1];
            System.arraycopy(newAlarm, 0, newAlarmWithLastValue, 0, newAlarm.length);
            newAlarmWithLastValue[newAlarm.length] = (byte) getChecksumRange(newAlarmWithLastValue, 0, newAlarmWithLastValue.length - 2);;;

            sendFirstData(gatt, newAlarmWithLastValue);

            // Log the new alarm array
            StringBuilder hexStringNewAlarm = new StringBuilder();
            for (byte b : newAlarmWithLastValue) {
                hexStringNewAlarm.append(String.format("%02X ", b)); // Convert byte to hex and append to string
            }
            Log.d(TAG, "result alarm New Alarm: " + hexStringNewAlarm.toString());

            // Create the alarmSecond array with the removed data
            byte[] alarmSecond = new byte[removedData.length + 7];
            alarmSecond[0] = (byte) 0xbb;
            alarmSecond[1] = (byte) 0x11;
            alarmSecond[2] = (byte) 0x10; // add 4 get all total
            alarmSecond[3] = (byte) 0xc7;
            alarmSecond[4] = (byte) 0x08;
            alarmSecond[5] = (byte) 0x00;
            alarmSecond[6] = (byte) 0x06; // is for request package type

            // Append removedData to alarmSecond starting at position 7
            System.arraycopy(removedData, 0, alarmSecond, 7, removedData.length);

            // Add the last value of alarmSecond to the array
            byte lastValueAlarmSecond = alarmSecond[alarmSecond.length - 1];
            Log.d(TAG, "Last Value of alarmSecond: " + String.format("%02X", lastValueAlarmSecond));
            byte[] alarmSecondWithLastValue = new byte[alarmSecond.length + 1];
            System.arraycopy(alarmSecond, 0, alarmSecondWithLastValue, 0, alarmSecond.length);
            alarmSecondWithLastValue[alarmSecond.length] = lastValueAlarmSecond;
            alarmSecondWithLastValue[alarmSecondWithLastValue.length - 1] = (byte) getChecksumRange(alarmSecondWithLastValue, 0, alarmSecondWithLastValue.length - 2);

            StringBuilder hexStringAlarmSecond = new StringBuilder();
            for (byte b : alarmSecondWithLastValue) {
                hexStringAlarmSecond.append(String.format("%02X ", b)); // Convert byte to hex and append to string
            }
            Log.d(TAG, "result alarm Updated Alarm Second: " + hexStringAlarmSecond.toString());

            try {
                Thread.sleep(3000);
                Log.e(TAG, "Received data set alarm for second data ------------ ");
                // Send the updated alarmSecond array
                sendFirstData(gatt, alarmSecondWithLastValue);

//                Thread.sleep(3000);
//                Log.e(TAG, "Received 4th tread ------------ ");
//                getAlarmValues(gatt);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } else {
            Log.e(TAG, "Value 0x63 not found in the array.");
        }

        Log.d(TAG, "result alarm: 1 " + hexString.toString());
    }



    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void sendFirstData(BluetoothGatt gatt, byte[] data) {

        for (BluetoothGattService gattService : gatt.getServices()) {
            // Loop through characteristics of this service
            for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                // Check if this characteristic is the one you're interested in
                if (gattCharacteristic.getUuid().equals(UUID.fromString("2F2DFFF5-2E85-649D-3545-3586428F5DA3"))) {
                    // React to the characteristic (e.g., read it, subscribe to notifications, etc.)
                    // Set write type if necessary
                    gattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    //byte[] _data = hexStringToByteArrayPillDispenser(modifiedHexString);
                    //first write
                    gattCharacteristic.setValue(data);
                    boolean writeSuccess = gatt.writeCharacteristic(gattCharacteristic);
                    if (!writeSuccess) {
                        Log.e(TAG, "Characteristic alarm write failed to initiate");
                        Log.e(TAG, "Characteristic write failed to initiate "+bytesToHexString(data));
                    }
                    else {
                        Log.e(TAG, "received Characteristic write value alarm "+ writeSuccess);
                    }
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void sendSecondData(BluetoothGatt gatt, byte[] secondWrite) {
        Log.e(TAG, "Received Data send second write");
        for (BluetoothGattService gattService : gatt.getServices()) {
            Log.d(TAG, "Service discovered 1: " + gattService.getUuid().toString());

            // Loop through characteristics of this service
            for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                Log.d(TAG, "Service Characteristic discovered: " + gattCharacteristic.getUuid().toString());
                Log.d(TAG, "Service Reading the notification value 2");
                // Check if this characteristic is the one you're interested in
                if (gattCharacteristic.getUuid().equals(UUID.fromString("2F2DFFF5-2E85-649D-3545-3586428F5DA3"))) {
                    Log.d(TAG, "Service Reading the notification value 3");
                    // React to the characteristic (e.g., read it, subscribe to notifications, etc.)
                    // Set write type if necessary
                    gattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    //byte[] _data = hexStringToByteArrayPillDispenser(modifiedHexString);
                    //first write
                    gattCharacteristic.setValue(secondWrite);
                    boolean secondWriteSuccess = gatt.writeCharacteristic(gattCharacteristic);
                    if (!secondWriteSuccess) {
                        Log.e(TAG, "Characteristic write failed to initiate secondWriteSuccess");
                    }
                    else {
                        Log.e(TAG, "Characteristic write value secondWriteSuccess "+ secondWriteSuccess);
                    }
                    //second write

                }
            }
        }
    }

    public int getChecksumRange(byte[] bData, int startIndex, int endIndex) {
        int cs = 0;
        for (int b = startIndex; b <= endIndex; b++) {
            cs += bData[b];
        }
        return cs & 0x00FF;
    }

    //Display a listing of all Medicine Schedules of patient
    public void getListMedicineSchedules() {
        // Usage example in your activity or service
        Log.d(TAG, "this is the result: getListMedicineSchedules "+serial);
        token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
        Call<MedicationSchedule> call = ApiClient.getUserService(Constant.BASE_URL_BGM,token, androidId).getMedicineSchedule();
        call.enqueue(new Callback<MedicationSchedule>() {
            @Override
            public void onResponse(Call<MedicationSchedule> call, Response<MedicationSchedule> response) {
                if (response.isSuccessful() && response.body() != null) {

                    // Get the current date
                    Calendar today = Calendar.getInstance();
                    // Define the formatter for day of the week in short form (e.g., Mon, Tue)
                    SimpleDateFormat formatter = new SimpleDateFormat("EEE", Locale.ENGLISH);
                    // Format the current date to get the day of the week
                    String dayOfWeek = formatter.format(today.getTime()).toLowerCase(Locale.ENGLISH);


                    // Get the MedicationSchedule object from the response
                    MedicationSchedule medicationSchedule = response.body();
                    List<MedicationDBValue> medicationDBValueList = new ArrayList<>();
                    // Access the list of MedicationData
                    databaseClient.getAppDatabase().medicationDBValueDao().clearAllMedications();
                    List<MedicationData> medicationDataList = medicationSchedule.getData();
                    for (MedicationData medicationData : medicationDataList) {
                        String id = medicationData.getId();
                        String name = medicationData.getName();
                        String description = medicationData.getDescription();
                        String type = medicationData.getType();
                        MainSchedule mainSchedule = medicationData.getMainSchedule();
                        if (mainSchedule != null) {
                            Day days = mainSchedule.getDays();
                            Log.d(TAG, "This is the Days: " + days.toString());
                            Log.d(TAG, "This is the Days: " + dayOfWeek);
                            // Get the value of the day from the Day class
                            boolean isDaySet = days.getDayValue(dayOfWeek);
                            Log.d(TAG, "This is the Days: isDaySet " + isDaySet);
                            if(isDaySet) {
                                List<ScheduleValue> scheduleValues = mainSchedule.getValue();
                                // Count the number of selected schedules
                                int selectedCount = 0;
                                for (ScheduleValue scheduleValue : scheduleValues) {
                                    if (scheduleValue.isSelected()) {
                                        selectedCount++;
                                    }
                                }
                                // Initialize alarm_hour array based on the number of selected schedules
                                // Each schedule requires 6 bytes
                                alarm_hour = new byte[selectedCount * 6];
                                int index = 0;
                                int tagHour = 0x61;
                                int tagMinutes = 0x71;
                                int setAlarmTag = 0x21; //for setting the first alarm
                                // Loop through the list and access each medication's details
                                int counter = 0;
                                for (ScheduleValue scheduleValue : scheduleValues) {
                                    String time = scheduleValue.getTime();
                                    String descriptionTime = scheduleValue.getDescription();
                                    int quantity = scheduleValue.getQuantity();
                                    boolean selected = scheduleValue.isSelected();
                                    if(selected) {
                                        Log.d(TAG, "Medication ID: " + id);
                                        Log.d(TAG, "Medication Name: " + name);
                                        Log.d(TAG, "Medication Description: " + description);
                                        // Log or use the schedule details
                                        Log.d(TAG, "Medication Schedule Selected: " + selected);
                                        Log.d(TAG, "Medication Schedule Time: " + time);
                                        int hour = Integer.parseInt(TimeConverter.getMilitaryHour(time));
                                        int minutes = Integer.parseInt(TimeConverter.getMilitaryMinutes(time));
//                                    Log.d(TAG, "Medication Schedule Time hours: " + TimeConverter.convertToHexadecimal(hour));
//                                    Log.d(TAG, "Medication Schedule Time minutes: " + TimeConverter.convertToHexadecimal(minutes));
                                        Log.d(TAG, "Medication Schedule Time hours: " + hour);
                                        Log.d(TAG, "Medication Schedule Time minutes: " + minutes);
                                        //set the Pill Dispenser
                                        // Populate the dynamic alarm_hour array
//                                    alarm_hour[index] = (byte) 0x61;  // Keep this fixed
                                        if(counter == 0) {
                                            alarm_hour[index] = (byte) tagHour;
                                            alarm_hour[index + 3] = (byte) tagMinutes;    // Keep this fixed
                                            setAlarmTag = 0x21;
                                            Log.d(TAG, "Medication setAlarm tag: 21 " + setAlarmTag);
                                        } else {
                                            tagHour = tagHour + 1;
                                            alarm_hour[index] = (byte) tagHour;  // Keep this fixed
                                            tagMinutes = tagMinutes + 1;
                                            alarm_hour[index + 3] = (byte) tagMinutes;    // Keep this fixed
                                            setAlarmTag = setAlarmTag + 1;

                                        }
                                        counter++;
                                        alarm_hour[index + 1] = (byte) 0x01;  // Keep this fixed
                                        alarm_hour[index + 2] = (byte) hour;    // Set the hour
                                        //alarm_hour[index + 2] = (byte) 22;    // Set the hour
                                        //minutes
                                        alarm_hour[index + 4] = (byte) 0x01;    // Keep this fixed
                                        alarm_hour[index + 5] = (byte) minutes; // Set the minutes
                                        //alarm_hour[index + 5] = (byte) 25; // Set the minutes
                                        //set the medication value for db
                                        MedicationDBValue medicationDBValue = new MedicationDBValue();
                                        medicationDBValue.setReferenceMedicineId(id);
                                        medicationDBValue.setMedicineSchedule(dayOfWeek);
                                        medicationDBValue.setName(name);
                                        medicationDBValue.setType(type);
                                        medicationDBValue.setAmount(scheduleValue.getQuantity());
                                        medicationDBValue.setUnit(medicationData.getUnit());
                                        medicationDBValue.setDateTaken(new Date());
                                        medicationDBValue.setDayTaken(dayOfWeek);
                                        medicationDBValue.setStateParameter(setAlarmTag);
                                        medicationDBValue.setConfigurationParameter(tagHour);
                                        medicationDBValue.setSelected(scheduleValue.isSelected());
                                        medicationDBValue.setParentDescription(medicationData.getDescription());
                                        medicationDBValue.setChildDescription(descriptionTime);
                                        medicationDBValue.setTime(scheduleValue.getTime());
                                        medicationDBValue.setTimeDisplay(scheduleValue.getTimeDisplay());
                                        medicationDBValue.setQuantity(quantity);
                                        medicationDBValueList.add(medicationDBValue);
                                        databaseClient.getAppDatabase().medicationDBValueDao().insertMedication(medicationDBValue);
                                        index += 6;  // Move to the next block of 6 bytes
                                        Log.d(TAG, "Medication setAlarm tag: " + setAlarmTag);
                                    }
                                }
                            }
                        }
                    }
                    //setAlarmTime(bluetoothGatt,alarm_hour, medicationDBValueList);
                }
            }

            @Override
            public void onFailure(Call<MedicationSchedule> call, Throwable throwable) {
                Log.d(TAG, "this is the result Fail getListMedicineSchedules"+ throwable);
            }
        });
    }

    // Method to convert byte array to hex string
    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            // Convert each byte to a hexadecimal string
            String hex = Integer.toHexString(b & 0xFF);
            // Pad single-digit hex values with a leading zero
            if (hex.length() < 2) {
                sb.append('0');
            }
            sb.append(hex);
        }

        // Return the final hex string
        return sb.toString().toLowerCase();  // To match the format (e.g., bb110dec...)
    }

    private boolean hasBtScanPerm() {
        return Build.VERSION.SDK_INT < 31 ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBtConnectPerm() {
        return Build.VERSION.SDK_INT < 31 ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void safeStopBleScan() {
        try {
            if (hasBtScanPerm() && bluetoothLeScanner != null) {
                //noinspection MissingPermission
                bluetoothLeScanner.stopScan(scanCallback);
            }
        } catch (SecurityException ignore) { /* no-op */ }
    }

    private void safeDisconnectAndCloseGatt() {
        try {
            if (bluetoothGatt != null) {
                if (hasBtConnectPerm()) {
                    //noinspection MissingPermission
                    bluetoothGatt.disconnect();
                    //noinspection MissingPermission
                    bluetoothGatt.close();
                }
                bluetoothGatt = null;
            }
        } catch (SecurityException ignore) { /* no-op */ }
    }

//    public void displayPage(String page) {
//        acquireWakeLock();
//        turnOnScreen(); // Turn on screen first
//
//        Log.e(TAG, "VALUE target_fragment: page " + page);
//
//        // First, ensure MainActivity is brought to front with proper flags
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.putExtra("target_page", page);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
//                Intent.FLAG_ACTIVITY_SINGLE_TOP |
//                Intent.FLAG_ACTIVITY_CLEAR_TOP |
//                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//
//        try {
//            startActivity(intent);
//
//            // Wait longer for screen to fully wake up and activity to be ready
//            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                // Send broadcast as backup
//                Intent navigationIntent = new Intent("com.monitor.health.NAVIGATE_TO_PAGE");
//                navigationIntent.putExtra("target_page", page);
//                sendBroadcast(navigationIntent);
//
//                // Also try sending another intent to MainActivity specifically
//                Intent fallbackIntent = new Intent(this, MainActivity.class);
//                fallbackIntent.putExtra("target_page", page);
//                fallbackIntent.putExtra("force_navigation", true);
//                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//                startActivity(fallbackIntent);
//
//            }, 2000); // Increased delay to 2 seconds
//
//        } catch (Exception e) {
//            Log.e(TAG, "Error starting activity: " + e.getMessage());
//        }
//    }

    public void displayPage(String page) {
        acquireWakeLock();
        Log.e(TAG, "VALUE target_fragment: pate " + page);
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("target_page", page);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }


    private void turnOnScreen() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        // Create a temporary wake lock to turn on the screen
        PowerManager.WakeLock screenWakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "com.monitor.health:ScreenWake"
        );

        screenWakeLock.acquire(3000); // Hold for 3 seconds

        // Release it after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (screenWakeLock.isHeld()) {
                screenWakeLock.release();
            }
        }, 3000);
    }

    public void checkBluetoothState() {

    }

    private void ensureScanner() {
        if (bluetoothLeScanner == null && bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
    }

    // Build ScanFilters so scanning can continue even when the screen is OFF.
    // Unfiltered scans may be paused by the OS when the screen turns off.
    private List<android.bluetooth.le.ScanFilter> scanFilters;

    private List<android.bluetooth.le.ScanFilter> getScanFilters() {
        if (scanFilters != null && !scanFilters.isEmpty()) return scanFilters;

        List<android.bluetooth.le.ScanFilter> filters = new ArrayList<>();

        // Common BLE profile services (helps keep the scan "filtered" and avoids screen-off throttling).
        // Add more UUIDs here if your target devices advertise other services.
        UUID THERMOMETER_SERVICE = UUID.fromString(Constant.THERMOMETER_SERVICE_UUID); // Health Thermometer
        //java.util.UUID WEIGHT_SCALE_SERVICE = java.util.UUID.fromString("fff0"); // Weight Scale
        UUID BLOOD_PRESSURE_SERVICE = UUID.fromString("fff0"); // Blood Pressure
        UUID GLUCOSE_SERVICE = UUID.fromString("1808"); // GLUCOSE
        UUID PULSE_OXIMETER_SERVICE = UUID.fromString(Constant.PULSE_OXIMETER_SERVICE_UUID); // Pulse Oximeter

        filters.add(new android.bluetooth.le.ScanFilter.Builder()
                .setServiceUuid(new android.os.ParcelUuid(THERMOMETER_SERVICE))
                .build());
//        filters.add(new android.bluetooth.le.ScanFilter.Builder()
//                .setServiceUuid(new android.os.ParcelUuid(WEIGHT_SCALE_SERVICE))
//                .build());
//        filters.add(new android.bluetooth.le.ScanFilter.Builder()
//                .setDeviceAddress("38:1E:C7:A7:52:83")
//                .build());
//
//        filters.add(new android.bluetooth.le.ScanFilter.Builder()
//                .setDeviceAddress("28:29:47:F3:3A:77")
//                .build());
        filters.add(new android.bluetooth.le.ScanFilter.Builder()
                .setDeviceName("JPD Scale")
                .build());

        filters.add(new android.bluetooth.le.ScanFilter.Builder()
                .setDeviceName("JPD BPM")
                .build());

        filters.add(new android.bluetooth.le.ScanFilter.Builder()
                .setDeviceName("My Thermometer")
                .build());

        filters.add(new android.bluetooth.le.ScanFilter.Builder()
                .setDeviceName("My Oximeter")
                .build());

        filters.add(new android.bluetooth.le.ScanFilter.Builder()
                .setDeviceName("EMPECS-BBXK010027")
                .build());



        filters.add(new android.bluetooth.le.ScanFilter.Builder()
                .setServiceUuid(new android.os.ParcelUuid(BLOOD_PRESSURE_SERVICE))
                .build());
        filters.add(new android.bluetooth.le.ScanFilter.Builder()
                .setServiceUuid(new android.os.ParcelUuid(GLUCOSE_SERVICE))
                .build());
        filters.add(new android.bluetooth.le.ScanFilter.Builder()
                .setServiceUuid(new android.os.ParcelUuid(PULSE_OXIMETER_SERVICE))
                .build());

        scanFilters = filters;
        return scanFilters;
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startScanSafe() {
        if (isScanning) return;
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "startScanSafe: BT disabled");
            return;
        }

        ensureScanner();
        if (bluetoothLeScanner == null) {
            Log.w(TAG, "startScanSafe: scanner is null");
            return;
        }

        try {
            lastResultAt = System.currentTimeMillis();

            // âœ… ADD: Use aggressive scan settings
            android.bluetooth.le.ScanSettings scanSettings = new android.bluetooth.le.ScanSettings.Builder()
                    .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY) // Most aggressive
                    .setCallbackType(android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(android.bluetooth.le.ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setReportDelay(0) // Report immediately
                    .build();

            // Start scan with settings
            startScanDynamic(scanSettings);
            isScanning = true;
            //Log.d(TAG, "BLE scan started (safe) with HIGH_PERFORMANCE mode");

            handlerScanForever.postDelayed(this::stopScanSafe, SCAN_WINDOW_MS);
            handlerScanForever.postDelayed(this::watchdogNoResults, NO_RESULT_RESET_MS);

        } catch (IllegalStateException e) {
            Log.w(TAG, "startScanSafe: adapter not ready", e);
            restartScannerWithBackoff();
        } catch (SecurityException se) {
            Log.w(TAG, "startScanSafe: missing permission?", se);
        }
    }

    private void stopScanSafe() {
        if (!isScanning) return;
        try {
            if (bluetoothLeScanner != null && bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                bluetoothLeScanner.stopScan(scanCallback);
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "stopScanSafe: adapter not ready", e);
        } catch (SecurityException ignore) {
        } finally {
            isScanning = false;
            // schedule next scan cycle
            handlerScanForever.postDelayed(this::startScanSafe, SCAN_PAUSE_MS);
        }
    }

    @SuppressLint("MissingPermission")
    private void watchdogNoResults() {
        if (!isScanning) return;
        long idle = System.currentTimeMillis() - lastResultAt;
        if (idle >= NO_RESULT_RESET_MS - 500) {
            Log.w(TAG, "No scan results for " + idle + "ms â†’ recycling scanner");
            try { if (bluetoothLeScanner != null) bluetoothLeScanner.stopScan(scanCallback); } catch (Exception ignore) {}
            bluetoothLeScanner = null; // force fresh instance
            // cool-down then restart
            handlerScanForever.postDelayed(this::startScanSafe, 1500);
        }
    }

    @SuppressLint("MissingPermission")
    private void restartScannerWithBackoff() {
        isScanning = false;
        handlerScanForever.removeCallbacksAndMessages(null);
        try { if (bluetoothLeScanner != null) bluetoothLeScanner.stopScan(scanCallback); } catch (Exception ignore) {}
        bluetoothLeScanner = null;
        handlerScanForever.postDelayed(this::startScanSafe, 2000);
    }

    // For indefinite wake lock (use carefully)
    private void acquireWakeLockIndefinite() {
        if (wakeLock == null) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "com.monitor.health:BleScanWakeLock"
            );
            wakeLock.setReferenceCounted(false);
        }

        if (!wakeLock.isHeld()) {
            wakeLock.acquire(); // Indefinite - be very careful!
            // Log.d(TAG, "Indefinite WakeLock acquired");
        }
    }


    private String normalizeMac(String value) {
        if (value == null) return null;
        value = value.trim();

        if (value.contains(":")) return value.toUpperCase(Locale.US);

        String raw = value.replace(":", "").toUpperCase(Locale.US);
        if (raw.length() != 12) return value;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i += 2) {
            sb.append(raw, i, i + 2);
            if (i < raw.length() - 2) sb.append(":");
        }
        return sb.toString();
    }



    @SuppressLint("MissingPermission")
    private void syncDevicesWithDatabase() {

        dbExecutor.execute(() -> {
            try {
                List<BleDeviceModel> dbDevices = databaseClient
                        .getAppDatabase()
                        .bleDeviceDao()
                        .getConnectedDevices(); // or getConnectedDevices()

                Set<String> dbMacs = new HashSet<>();

                if (dbDevices != null) {
                    for (BleDeviceModel m : dbDevices) {
                        String mac = normalizeMac(m.getSerial());
                        if (mac != null && BluetoothAdapter.checkBluetoothAddress(mac)) {
                            dbMacs.add(mac.toUpperCase());
                        }
                    }
                }

                main.post(() -> {

                    // 1ï¸âƒ£ DISCONNECT devices not in DB
                    for (String connectedMac : new HashSet<>(connectedGattMap.keySet())) {
                        if (!dbMacs.contains(connectedMac)) {
                            Log.d(TAG, "Disconnecting (not in DB): " + connectedMac);

                            BluetoothGatt gatt = connectedGattMap.get(connectedMac);
                            if (gatt != null) {
                                gatt.disconnect();
                                gatt.close();
                            }

                            connectedGattMap.remove(connectedMac);
                        }
                    }

                    // 2ï¸âƒ£ CONNECT devices that are in DB but not connected
                    for (String dbMac : dbMacs) {
                        if (!connectedGattMap.containsKey(dbMac)) {

                            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                                continue;
                            }

                            Log.d(TAG, "Connecting (exists in DB): " + dbMac);

                            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(dbMac);
                            BluetoothGatt gatt = device.connectGatt(
                                    BleScanService2.this,
                                    false,
                                    createGattCallback(dbMac)
                            );

                            connectedGattMap.put(dbMac, gatt);
                        }
                    }

                });

            } catch (Exception e) {
                Log.e(TAG, "syncDevicesWithDatabase error", e);
            }
        });
    }

    private BluetoothGattCallback createGattCallback(String mac) {
        return new BluetoothGattCallback() {

            @SuppressLint("MissingPermission")
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Connected: " + mac);
                    gatt.discoverServices();

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected: " + mac);

                    connectedGattMap.remove(mac);

                    try { gatt.close(); } catch (Exception ignored) {}
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.d(TAG, "Services discovered for: " + mac);
            }
        };
    }

}