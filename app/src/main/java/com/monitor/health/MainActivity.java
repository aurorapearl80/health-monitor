package com.monitor.health;

import static com.monitor.health.Constant.ACTION_HEALTH_UPDATE;
import static com.monitor.health.utility.AppUtils.getTodayDate;
import static com.monitor.health.utility.LauncherItems.ACTION_BLOODO;
import static com.monitor.health.utility.LauncherItems.ACTION_BLOOD_CLASE;
import static com.monitor.health.utility.LauncherItems.ACTION_HEART;
import static com.monitor.health.utility.LauncherItems.ACTION_HEART_CLASE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.HSystemAssistManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.monitor.health.adapter.HealthManager;
import com.monitor.health.adapter.PageType;
import com.monitor.health.adapter.ScreenSlidePagerAdapter;
import com.monitor.health.adapter.SmartWatchPageTransformer;
import com.monitor.health.dao.BPJumperDao;
import com.monitor.health.dao.FileResponseDto;
import com.monitor.health.dao.HeartRateDao;
import com.monitor.health.dao.OximeterDao;
import com.monitor.health.dao.TemperatureDao;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.entity.TypeAvailabilityEntity;
import com.monitor.health.model.BPJumper;
import com.monitor.health.model.BleDeviceModel;
import com.monitor.health.model.Day;
import com.monitor.health.model.Frequency;
import com.monitor.health.model.HeartRateEntity;
import com.monitor.health.model.MainSchedule;
import com.monitor.health.model.MedicationDBValue;
import com.monitor.health.model.MedicationData;
import com.monitor.health.model.MedicationSchedule;
import com.monitor.health.model.Oximeter;
import com.monitor.health.model.Reading;
import com.monitor.health.model.ReadingValue;
import com.monitor.health.model.ScheduleValue;
import com.monitor.health.model.Temperature;
import com.monitor.health.model.healthscore.DataObjectDto;
import com.monitor.health.model.healthscore.UserDrWatch;
import com.monitor.health.receiver.HourlyKickReceiver;
import com.monitor.health.request.SendAlarmRequest;
import com.monitor.health.response.FrequencyResponse;
import com.monitor.health.response.bledevice.BLEData;
import com.monitor.health.response.bledevice.DeviceDetails;
import com.monitor.health.response.bledevice.DeviceResponse;
import com.monitor.health.response.bledevice.DeviceResponseList;
import com.monitor.health.response.user.UserProfileResponse;
import com.monitor.health.services.BackButtonInterceptorService;
import com.monitor.health.services.BleScanService;
import com.monitor.health.services.MyForegroundService;
import com.monitor.health.services.BloodOxygenSensorService;
import com.monitor.health.services.FallDetectionService;
import com.monitor.health.services.HeartRateSensorService;
import com.monitor.health.services.HeartRateServiceNative;
import com.monitor.health.services.HomeDoubleTapService;
import com.monitor.health.services.KeyMonitorAccessibilityService;
import com.monitor.health.services.LocationService;
import com.monitor.health.services.StepsService;
import com.monitor.health.services.TestService;
import com.monitor.health.ui.TemperatureFragment;
import com.monitor.health.utility.ApkDownloadManager;
import com.monitor.health.utility.BodyComposition;
import com.monitor.health.utility.DateUtils;
import com.monitor.health.utility.DeviceUtils;
import com.monitor.health.utility.PreferenceHelper;
import com.monitor.health.utility.SmartWatchAlertDialog;
import com.monitor.health.utility.TimeAgo;
import com.monitor.health.utility.TimeConverter;
import com.monitor.health.viewmodel.ReadingsViewModel;
import com.monitor.health.viewmodel.SharedDataViewModel;
import com.monitor.health.worker.ServiceKeepaliveWorker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.os.PowerManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.monitor.health.response.alldata.TypesAvailability;
import java.util.Collections;


public class MainActivity extends AppCompatActivity  implements StepsService.SensorDataListener {

    /** True only while MainActivity is in the resumed (visible) state.
     *  Read by KeyMonitorAccessibilityService to avoid launching the SOS dialog
     *  when the app is no longer in the foreground. */
    public static volatile boolean isInForeground = false;

    private PowerManager.WakeLock wakeLock;
    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSIONS = 1001;
    private boolean isNavigationReceiverRegistered = false;

    UserDrWatch userDrWatch;

    String _model;
    String _maker;
    String osVersion;
    String _country;
    String androidId;

//    Button btnSos;
//    TextView txtUsername,txtEmail, txtAddress, txtPhone,
//            heartRateText, txtAndroidId, txtSteps, txtBloodOxygen, txtIotUpdate;
    private static final int MAX_RETRY_COUNT = 3; // Set max retry attempts
    private int retryCount = 0;
    private final java.util.concurrent.ExecutorService syncExecutor =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    int batteryPercent = 0;

    private float lastSentHeartRate = -1;
    private long lastSentTime = 0;
    private double latitude = 0;
    private double longitude = 0;

    SharedPreferences prefs;

    private BleScanService bleScanService;
    private boolean isBound = false;

    public final static String ACTION_IOTSERVICES = "android.hsc.iotservices";
    public final static String ACTION = "action";

    private ViewPager2 viewPager;
    private View loadingOverlay;

    private ScreenSlidePagerAdapter pagerAdapter;

    SharedDataViewModel model;
    ReadingsViewModel readingsViewModel;

    private HSystemAssistManager systemAssistManager;

    DatabaseClient databaseClient;

    private HeartRateSensorService mService;
    private boolean mBound = false;

    private BloodOxygenSensorService mServiceBloodOxygen;
    private boolean mBoundOxygen = false;


    //Steps
    private StepsService sensorService;
    private boolean serviceBound = false;

    //Heart rate and
    private SensorManager mSensorManager;
    private static final int TYPE_HEART_RATE = 21;

    // Measurement variables
    private int heartRateValue = 0;
    private int bloodRateValue = 0;
    private int steps;
    private Sensor mSensor;
    private float semaphore;
    private float light;

    private Handler handler = new Handler();
    private Runnable runnable;
    private static final int KEYCODE_HOME = KeyEvent.KEYCODE_HOME;
    private static final long DOUBLE_TAP_DELAY = 300; // max delay in ms between taps
    private long lastTapTime = 0;

    private List<PageType> visiblePages = new ArrayList<>();

    private BroadcastReceiver navigationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.monitor.health.NAVIGATE_TO_PAGE".equals(intent.getAction())) {
                String target = intent.getStringExtra("target_page");
                if (target != null) {
                    Log.d(TAG, "Backup navigation triggered for: " + target);
                    // Create a mock intent for existing navigation logic
                    Intent navigationIntent = new Intent();
                    navigationIntent.putExtra("target_page", target);
                    handleNavigation(navigationIntent);
                }
            }
        }
    };


    // Matches the broadcast from TestServic

    private final BroadcastReceiver healthUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !ACTION_HEALTH_UPDATE.equals(intent.getAction())) return;

            int heart = intent.getIntExtra("heartRate", 0);
            int blood = intent.getIntExtra("bloodRate", 0);
            int steps = intent.getIntExtra("steps", -1);

            //Log.d(TAG, "Update â†’ heart=" + heart + ", blood=" + blood + ", steps=" + steps);

            //tvHeartRate.setText("Heart Rate: " + heart + " BPM");
            //tvBloodRate.setText("Blood Rate: " + blood);
            //tvSteps.setText(steps >= 0 ? "Steps: " + steps : "Steps: --");
            model.setHeartRateMonitor(heart);
            model.setOxygen(blood);
            model.setStepCount(steps);

        }
    };


    // Service connection Steps
    private ServiceConnection serviceConnectionSteps = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            StepsService.LocalBinder binder = (StepsService.LocalBinder) service;
            sensorService = binder.getService();
            serviceBound = true;

            // Set this activity as listener for direct updates
            sensorService.setSensorDataListener(MainActivity.this);

            // Request immediate data update
            sensorService.requestDataUpdate();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected");
            serviceBound = false;
            sensorService = null;
        }
    };

    // Broadcast receiver for sensor data updates
    private BroadcastReceiver sensorDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StepsService.ACTION_SENSOR_DATA.equals(intent.getAction())) {
                int steps = intent.getIntExtra(StepsService.EXTRA_STEPS, 0);
                float semaphore = intent.getFloatExtra(StepsService.EXTRA_SEMAPHORE, 0f);
                float light = intent.getFloatExtra(StepsService.EXTRA_LIGHT, 0f);
                int stepCount = intent.getIntExtra(StepsService.EXTRA_STEP_COUNT, 0);
                model.setStepCount(stepCount);
                //updateUI(steps, semaphore, light, stepCount);
            }
        }
    };
    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !intent.hasExtra(ACTION)) {
                Log.d(TAG, "intent == null or not action!");
                return;
            }
            int action = intent.getIntExtra(ACTION, -1);
            //Log.d(TAG, "action:" + action);
            if (action != -1) {
                if (action == ACTION_HEART) {
                    int heart = intent.getIntExtra("heart", 0);
                    // heartResult.setText(heart + "");
                   // heartRateText.setText("Heart Rate: " + heart + " bpm");
                }if (action == ACTION_BLOODO) {
                    int bloodo = intent.getIntExtra("blood", 0);
                    // bloodResult.setText(bloodo + "");
                    //txtBloodOxygen.setText("Blood Rate: " + bloodo);
                }if (action == ACTION_HEART_CLASE) {
                    Toast.makeText(MainActivity.this, "heart clase", Toast.LENGTH_LONG).show();
                }if (action == ACTION_BLOOD_CLASE) {
                    Toast.makeText(MainActivity.this, "blood clase", Toast.LENGTH_LONG).show();
                }
            }


        }
    };


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BleScanService.LocalBinder binder = (BleScanService.LocalBinder) service;
            bleScanService = binder.getService();
            isBound = true;

            // Now you can call your method
            bleScanService.startForeverBleScan();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

//    private final BroadcastReceiver heartRateReceiver = new BroadcastReceiver() {
//        @SuppressLint("SetTextI18n")
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if ("com.monitor.health.HEART_RATE_UPDATE".equals(action)) {
//                float heartRate = intent.getFloatExtra("heartRate", 0);
////                heartRateText.setText("Heart Rate: " + (int) heartRate + " bpm");
//                //sendHeartRate(heartRate);
//            }
//            if ("com.monitor.health.STEP_UPDATE".equals(action)) {
//                int steps = intent.getIntExtra("steps", 0);
//                txtSteps.setText("Steps: +" + steps);
//            }
//        }
//    };

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            latitude = intent.getDoubleExtra("lat", 0);
            longitude = intent.getDoubleExtra("lng", 0);
            String locationStr = "Lat: " + latitude + "\nLng: " + longitude;

            // Get SharedPreferences (private mode means it's only for your app)
// Edit and save
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("latitude", Double.doubleToRawLongBits(latitude));
            editor.putLong("longitude", Double.doubleToRawLongBits(longitude));
            editor.apply();  // or .commit() if you want synchronous saving
            //txtSteps.setText(locationStr);
            //Log.d("MainActivity", locationStr);
        }
    };

    // In MainActivity.java

    private BroadcastReceiver bleTemperature = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.ACTION_TEMPERATURE.equals(intent.getAction())) {
                double acc = intent.getDoubleExtra(Constant.VALUE_TEMPERATURE, 0f);
                Log.d(TAG, "The data of temperature main "+acc);
                //model.setHeartRate(acc);
                model.setTemperatureData(acc);
            }
        }
    };

    private BroadcastReceiver bleOximeter = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.ACTION_PULSE_OXIMETER.equals(intent.getAction())) {
                int pulseRate = intent.getIntExtra(Constant.VALUE_PULSE_OXIMETER_PULSE_RATE, 0);
                int oxygen = intent.getIntExtra(Constant.VALUE_OXIMETER_PULSE_OXYGEN, 0);
                Log.d(TAG, "sendOximeter main The data of pulseRate main "+pulseRate);
                //model.setHeartRate(acc);
                List<Integer> oximeters = new ArrayList<>();
                oximeters.add(pulseRate);
                oximeters.add(oxygen);
                model.setOximeterData(oximeters);
            }
        }
    };

    private BroadcastReceiver bleGlucose = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.ACTION_BLOOD_GLUCOSE.equals(intent.getAction())) {
                int glucose = intent.getIntExtra(Constant.VALUE_BLOOD_GLUCOSE, 0);
                Log.d(TAG, "The data of glucose main"+glucose);
                String todayIsoUtc = LocalDate.now(ZoneOffset.UTC)
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC)
                        .toString();
                model.setGlucoseCreatedAt(TimeAgo.relativeFromIsoUtc(todayIsoUtc));
                model.setGlucoseData(glucose);


            }
        }
    };

    //Jar file background process
    private BroadcastReceiver heartRateFromJarFile = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.ACTION_HEART_RATE_DATA.equals(intent.getAction())) {
                int heartRate = (int) intent.getFloatExtra(Constant.EXTRA_HEART_RATE, 0);
                int currentMode = intent.getIntExtra(Constant.EXTRA_SENSOR_MODE, 0);
                //Log.d(TAG, "HeartRateService The data of heart rate main"+heartRate);
                //Log.d(TAG, "HeartRateService The data of heart rate main"+heartRate);
                //model.setGlucoseData(heartRate);
                model.setHeartRateMonitor(heartRate);

            }
        }
    };

    private BroadcastReceiver oxygenFromJarFile = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.ACTION_BLOOD_OXYGEN_DATA.equals(intent.getAction())) {
                int oxygen = (int) intent.getFloatExtra(Constant.EXTRA_BLOOD_OXYGEN_VALUE, 0);
                int currentMode = intent.getIntExtra(Constant.EXTRA_SENSOR_MODE, 0);
                //Log.d(TAG, "HeartRateService The data of heart rate main"+heartRate);
                //Log.d(TAG, "HeartRateService The data of heart rate main"+heartRate);
                model.setOxygen(oxygen);

            }
        }
    };


    BroadcastReceiver bloodPressureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.ACTION_BLOOD_PRESSURE.equals(intent.getAction())) {
                ArrayList<Double> bloodpressureData =
                        (ArrayList<Double>) intent.getSerializableExtra(Constant.VALUE_BLOOD_PRESSURE);

                if (bloodpressureData != null) {
                    double systolic = bloodpressureData.get(0);
                    double diastolic = bloodpressureData.get(1);
                    double bp = bloodpressureData.get(2);
                    Log.d("BP---", "Systolic: " + systolic + " Diastolic: " + diastolic +"Blood pressure "+bp);
                    model.setBloodPressureData(bloodpressureData);
                }
            }
        }
    };

    private BroadcastReceiver bleWeight = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.ACTION_WEIGHT.equals(intent.getAction())) {
                Double weight = intent.getDoubleExtra(Constant.VALUE_WEIGHT, 0);
                Log.d(TAG, "weight get activity of temperature main"+weight);
                //model.setHeartRate(acc);
                model.setWeightData(weight);
            }
        }
    };

    private final BroadcastReceiver heartRateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (HeartRateServiceNative.ACTION_HEART_RATE.equals(intent.getAction())) {
                int heartRate = intent.getIntExtra("heart_rate", -1);
                int bloodValue = intent.getIntExtra("blood_value", -1);
                model.setHeartRateExternal(heartRate);
                model.setBloodValueExternal(bloodValue);

               // Log.d("MainActivity", "Heart Rate: " + heartRate + ", Blood Value: " + bloodValue);
                //Toast.makeText(context, "Heart Rate: " + heartRate + ", Blood Value: " + bloodValue, Toast.LENGTH_SHORT).show();
                // TODO: update UI or handle values
            }
        }
    };

    private BroadcastReceiver heartRateMonitorFromJar = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.ACTION_HEART_RATE_MONITOR_FROM_JAR.equals(intent.getAction())) {
                int heart = intent.getIntExtra(Constant.VALUE_HEART_RATE_MONITOR_FROM_JAR, 0);
                Log.d(TAG, "The data of temperature main"+heart);
                //model.setHeartRate(acc);
                model.setHeartRateMonitor(heart);
            }
        }
    };

    private BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("state");
            if ("OFF".equals(state)) {
                showBluetoothOffDialog();
            }
//            if ("ON".equals(state)) {
//                //startBleService();
//            }
        }
    };

    private SensorEventListener mHeartRateListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            steps = (int) (event.values[0] + 0.5f);

            //Log.d(TAG, "Sensor value received: " + steps);

            // Try to extract both heart rate and blood rate from sensor data
            heartRateValue = steps; // Primary sensor value as heart rate

            // Check if there are additional values in the sensor event
            if (event.values.length > 1) {
                bloodRateValue = (int) (event.values[1] + 0.5f);
                //Log.d(TAG, "Blood rate from sensor[1]: " + bloodRateValue);
            }

            // Alternative: Try to get blood rate from different sensor indices
            if (event.values.length > 2) {
                int alternativeBlood = (int) (event.values[2] + 0.5f);
                if (alternativeBlood > 0 && alternativeBlood != heartRateValue) {
                    bloodRateValue = alternativeBlood;
                    //Log.d(TAG, "Blood rate from sensor[2]: " + bloodRateValue);
                }
            }

            // Get additional sensor values
            if (event.values.length > 5) {
                semaphore = event.values[5];
            }
            if (event.values.length > 6) {
                light = event.values[6];
            }

            //Log.d(TAG, "Heart Rate: " + heartRateValue + ", Blood Rate: " + bloodRateValue);
            handler.post(runnable);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "Sensor accuracy changed: " + accuracy);
        }
    };

    @SuppressLint({"HardwareIds", "UnspecifiedRegisterReceiverFlag", "WrongConstant", "MissingPermission", "ObsoleteSdkInt", "BatteryLife"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true);
        }
        // Keep screen on at minimal brightness for medical monitoring
        // For medical kiosk - keep screen on


        getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );


        setContentView(R.layout.activity_main);
        model = new ViewModelProvider(this).get(SharedDataViewModel.class);
        readingsViewModel = new ViewModelProvider(this).get(ReadingsViewModel.class);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

//        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//        intent.setData(Uri.parse("package:" + getPackageName()));
//        startActivity(intent);



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED) {
            BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bm != null && bm.getAdapter() != null) {
                bm.getAdapter().setName("MyApp_BLE_Debug");
            }
        }

        prefs = getSharedPreferences("LocationPrefs", MODE_PRIVATE);
        //sosProgress = findViewById(R.id.sosProgress);
        Log.wtf("BatteryPercent", "connecting------------------");
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, filter);
        assert batteryStatus != null;
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        batteryPercent = (level * 100) / scale;

        databaseClient = DatabaseClient.getInstance(this);
        userDrWatch = new UserDrWatch();

        //androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        //Log.d("Android----------------------", "ID: "+androidId);
        androidId = DeviceUtils.getIMEI(this);
        //Log.d("Android", "ID test: "+androidId);
        //txtAndroidId.setText(androidId);

        _model = Build.MODEL;
        _maker = Build.MANUFACTURER;
        osVersion = Build.VERSION.RELEASE;
        _country = Locale.getDefault().getCountry();

        // Start the service, e.g., in onCreate()

        ContextCompat.registerReceiver(this, bleTemperature, new IntentFilter(Constant.ACTION_TEMPERATURE), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, bleGlucose, new IntentFilter(Constant.ACTION_BLOOD_GLUCOSE), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, bleOximeter, new IntentFilter(Constant.ACTION_PULSE_OXIMETER), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, bloodPressureReceiver, new IntentFilter(Constant.ACTION_BLOOD_PRESSURE), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, bleWeight, new IntentFilter(Constant.ACTION_WEIGHT), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, healthUpdateReceiver, new IntentFilter(Constant.ACTION_WEIGHT), ContextCompat.RECEIVER_NOT_EXPORTED);
        //Jar File
       // registerReceiver(heartRateFromJarFile, new IntentFilter(Constant.ACTION_HEART_RATE_DATA));
        //registerReceiver(oxygenFromJarFile, new IntentFilter(Constant.ACTION_BLOOD_OXYGEN_DATA));
        //registerReceiver(heartRateMonitorFromJar, new IntentFilter(Constant.ACTION_HEART_RATE_MONITOR_FROM_JAR));



        //heart Rate services:
//        Intent serviceIntent2 = new Intent(this, HeartRateService.class);
//        startService(serviceIntent2);





        //loginDrWatch();
        ///userProfile();

        ContextCompat.registerReceiver(this, locationReceiver, new IntentFilter("LOCATION_UPDATE"), ContextCompat.RECEIVER_NOT_EXPORTED);

        //logDeviceInfo();
        checkAndRequestBatteryOptimization();

        requestAllPermissionsIfNeeded();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intentWhiteList = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intentWhiteList);
            }
        }
        //Log.d(TAG, DeviceUtils.getIMEI(this)+" the IME");

        viewPager = findViewById(R.id.viewPager);
        loadingOverlay = findViewById(R.id.loadingOverlay);
       // viewPager.setOffscreenPageLimit(3);
        //viewPager.setClipToPadding(false);
        //viewPager.setClipChildren(false);

//        pagerAdapter = new ScreenSlidePagerAdapter(this);
//        viewPager.setAdapter(pagerAdapter);

        pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

// âœ… show default pages first (before API returns)
        pagerAdapter.setPages(defaultPages());

        // Add page transformer for smooth animations
        //viewPager.setPageTransformer(new SmartWatchPageTransformer());
        // Optional: Add auto-scroll for demo purposes
       //setupAutoScroll();



        model.setImeData(DeviceUtils.getIMEI(this));


        //String downloadUrl = "https://drive.google.com/uc?export=download&id=1yFOr03N70nLHSumoy23xAdNq6N52VbMs";
        //String downloadUrl = "https://sample-file.bazadanni.com/download/applications/android/sample.apk";
        //downloadAndInstallApk(downloadUrl);

        //checkVersionDownload();

        // In MainActivity onCreate or where appropriate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }


        //For testing
        //Glocuse
        //saveData(12, 1, "serial");
        testData();
        //testHrandSpo2();


        //initializeViews();
        initializeSystemServices();
        setupSensorListener();
        ///registerBroadcastReceiver();

        // Enable both heart rate and blood rate monitoring simultaneously
        //enableBothMeasurements();
        updateUI();

        getFrequencySettings();
        //setTimer(1, "min");
        // Example after parsing the item you want (e.g., Heart Rate):
        //int interval = item.getMeasurementInterval();      // e.g., 1 or 2
        ///String unit = item.getIntervalUnit();              // e.g., "min" or "minutes" or "hr"
//        int interval = 1;      // e.g., 1 or 2
//        String unit = "min";              // e.g., "min" or "minutes" or "hr"
//        HourlyKickReceiver.setIntervalFromServer(this, interval, unit);


//        ApkDownloadManager manager = new ApkDownloadManager(this);
//        manager.setupCallbacks(); // Set up progress and installation callbacks
//        manager.checkVersionDownload(); // Start the process

//        setUpFcm();
        handleNavigation(getIntent());

        // Schedule it:
        PeriodicWorkRequest keepaliveWork = new PeriodicWorkRequest.Builder(
                ServiceKeepaliveWorker.class, 15, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "ServiceKeepalive", ExistingPeriodicWorkPolicy.KEEP, keepaliveWork);


        //sendBloodPressureRateSync(123,48);

       // HourlyKickReceiver.setIntervalFromServer(this, 15, "min");
        //HourlyKickReceiver.setIntervalFromServer(this, 15, "min");
        //HourlyKickReceiver.setIntervalFromServer(this, 15, "minutes");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean ignoring = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ignoring = pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        Log.d("DeviceInfo", "Manufacturer=" + Build.MANUFACTURER
                + ", Model=" + Build.MODEL
                + ", SDK=" + Build.VERSION.SDK_INT
                + ", ignoringBatteryOpt=" + ignoring);



        readingsViewModel.fetchLatest(
                Constant.BASE_URL_BGM,
                "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%",
                DeviceUtils.getIMEI(this)
        );

        loadingOverlay.setVisibility(View.VISIBLE);

        if (NetworkUtils.isInternetConnected(getApplicationContext())) {
            //Get the latest data in local and send to the server that has a value of status 1
            //Log.d(TAG, "Internet connection sent the data temperature");
            readingsViewModel.getTypesAvailabilityMutableLiveData()
                    .observe(this, typesAvailability -> {
                        if (typesAvailability == null) return;
                        //Clean first
                        databaseClient.getAppDatabase().typeAvailabilityDao().deleteAll();
                        //Insert in the data
                        databaseClient.getAppDatabase().typeAvailabilityDao().insertTypeAvailability(new TypeAvailabilityEntity(
                                typesAvailability.isBloodGlucose(),
                                typesAvailability.isBloodPressure(),
                                typesAvailability.isWeight(),
                                typesAvailability.isBloodOxygen(),
                                typesAvailability.isElectrocardiogram(),
                                typesAvailability.isTemperature()
                        ));
                        // Build pages dynamically
                        List<PageType> pages = buildPages(typesAvailability);

                        Log.wtf("AVAILABLE", typesAvailability.toString());
                        Log.wtf("PAGES_COUNT", String.valueOf(pages.size()));

                        // Update adapter pages
                        pagerAdapter.setPages(pages);

                        // safety
                        if (viewPager.getCurrentItem() >= pages.size()) {
                            viewPager.setCurrentItem(0, false);
                        }

                        loadingOverlay.setVisibility(View.GONE);
                    });
        }
        else {
            //Log.d(TAG, "No internet connection from BLE Services");
            //saveOximeter(stableInt, oxygen, 1, serial);
            TypeAvailabilityEntity typeAvailabilityEntity = databaseClient.getAppDatabase().typeAvailabilityDao().getSingleTypeAvailability();

            if (typeAvailabilityEntity == null) {
                typeAvailabilityEntity = new TypeAvailabilityEntity();
            }

            List<PageType> pages = buildPages(new TypesAvailability(


                    typeAvailabilityEntity.isBloodGlucose(),
                    typeAvailabilityEntity.isBloodPressure(),
                    typeAvailabilityEntity.isWeight(),
                    typeAvailabilityEntity.isBloodOxygen(),
                    typeAvailabilityEntity.isElectrocardiogram(),
                    typeAvailabilityEntity.isTemperature()
            ));

            // Update adapter pages
            pagerAdapter.setPages(pages);
            // safety
            if (viewPager.getCurrentItem() >= pages.size()) {
                viewPager.setCurrentItem(0, false);
            }

            loadingOverlay.setVisibility(View.GONE);
        }

        // If the server times out (SocketTimeoutException) or returns an error while online,
        // the typesAvailability observer never fires â€” loader would hang forever.
        // Observe the error LiveData to hide the loader and fall back to cached DB data.
        readingsViewModel.getError().observe(this, errorMsg -> {
            if (errorMsg == null) return;
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this,
                    "Connection is slow or there is a problem connecting to the server. Please try again.",
                    Toast.LENGTH_LONG).show();

            // Fall back to last cached types so the UI is still usable
            TypeAvailabilityEntity cached = databaseClient.getAppDatabase()
                    .typeAvailabilityDao().getSingleTypeAvailability();
            if (cached == null) cached = new TypeAvailabilityEntity();
            List<PageType> pages = buildPages(new TypesAvailability(
                    cached.isBloodGlucose(),
                    cached.isBloodPressure(),
                    cached.isWeight(),
                    cached.isBloodOxygen(),
                    cached.isElectrocardiogram(),
                    cached.isTemperature()
            ));
            pagerAdapter.setPages(pages);
            if (viewPager.getCurrentItem() >= pages.size()) {
                viewPager.setCurrentItem(0, false);
            }
        });

        //Please don't forget this to upload -- Warning
//       if (!isAccessibilityServiceEnabled(this, KeyMonitorAccessibilityService.class)) {
//            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
//        } else {
//            Log.d("Key-change", "Accessibility already enabled. No popup.");
//        }
        //databaseClient.getAppDatabase().bleDeviceDao().deleteAll();
        //getAssignDevices();
        //databaseClient.getAppDatabase().bleDeviceDao().resetAllConnections();

        checkAccessibilityServices();

    }

    private void checkAccessibilityServices() {
        String service = getPackageName() + "/" +
                KeyMonitorAccessibilityService.class.getCanonicalName();

        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        Log.d("AccessibilityCheck", "Enabled services: " + enabledServices);

        boolean isEnabled = false;

        if (enabledServices != null) {
            isEnabled = enabledServices.contains(service);
        }

        Log.d("AccessibilityCheck", "KeyMonitorAccessibilityService enabled: " + isEnabled);
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> serviceClass) {
        String expected = context.getPackageName() + "/" + serviceClass.getName();

        int enabled = 0;
        try {
            enabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }

        if (enabled != 1) return false;

        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        if (enabledServices == null) return false;

        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);

        while (splitter.hasNext()) {
            String s = splitter.next();
            if (s.equalsIgnoreCase(expected)) {
                return true;
            }
        }
        return false;
    }






    private void logDeviceInfo() {
        Log.d("MainActivity", "=== DEVICE INFO ===");
        Log.d("MainActivity", "Manufacturer: " + Build.MANUFACTURER);
        Log.d("MainActivity", "Model: " + Build.MODEL);
        Log.d("MainActivity", "Android Version: " + Build.VERSION.SDK_INT);
        Log.d("MainActivity", "==================");
    }

    private void checkAndRequestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            String packageName = getPackageName();

            boolean isIgnoring = pm.isIgnoringBatteryOptimizations(packageName);
            Log.d("MainActivity", "Battery optimization disabled: " + isIgnoring);

            if (isIgnoring) return;

            android.content.SharedPreferences prefs =
                    getSharedPreferences("app_prefs", MODE_PRIVATE);
            boolean alreadyPrompted = prefs.getBoolean("battery_opt_prompted", false);
            if (alreadyPrompted) return;

            new AlertDialog.Builder(this)
                    .setTitle("Enable Continuous Monitoring")
                    .setMessage("This medical device app needs to run continuously. Please disable battery optimization.")
                    .setPositiveButton("Go to Settings", (dialog, which) -> {
                        prefs.edit().putBoolean("battery_opt_prompted", true).apply();
                        try {
                            Intent intent = new Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:" + packageName));
                            startActivity(intent);
                        } catch (Exception e) {
                            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                        }
                    })
                    .setNegativeButton("Not now", (dialog, which) ->
                            prefs.edit().putBoolean("battery_opt_prompted", true).apply())
                    .setCancelable(false)
                    .show();
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //setIntent(intent);
        // Handle subsequent intents when activity is already on top
        handleNavigation(intent);
    }

//    private void handleNavigation(Intent intent) {
//        if (intent == null || !intent.hasExtra("target_fragment")) return;
//
//        int targetPage = intent.getIntExtra("target_fragment", -1);
//        if (targetPage < 0) return;
//
//        int currentPage = viewPager.getCurrentItem();
//        if (currentPage == targetPage) {
//            // Already on this page â€” do nothing (prevents reload/flicker)
//            Log.d("MainActivity", "Already on page " + targetPage + ", skip navigation.");
//            return;
//        }
//
//        // Navigate only if different
//        viewPager.setCurrentItem(targetPage, false);
//    }

//    private void handleNavigation(Intent intent) {
//        if (intent == null) return;
//
//        String target = intent.getStringExtra("target_page");
//        if (target == null) return;
//
//        try {
//            PageType type = PageType.valueOf(target);
//            int index = indexOfPage(type);
//            if (index >= 0) {
//                // Check if we're in the right lifecycle state
//                if (isFinishing() || isDestroyed()) {
//                    Log.w(TAG, "Activity is finishing/destroyed, skipping navigation");
//                    return;
//                }
//
//                // Ensure ViewPager is ready with multiple checks
//                Handler mainHandler = new Handler(Looper.getMainLooper());
//
//                // First attempt - immediate
//                if (viewPager != null && viewPager.getAdapter() != null) {
//                    navigateToPageSafely(index, target, 0);
//                } else {
//                    // Wait for ViewPager to be ready
//                    mainHandler.postDelayed(() -> navigateToPageSafely(index, target, 1), 500);
//                }
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Invalid target_page: " + target, e);
//        }
//    }
private void handleNavigation(Intent intent) {
    if (intent == null) return;

    String target = intent.getStringExtra("target_page");
    if (target == null) return;

    try {
        PageType type = PageType.valueOf(target);
        int index = indexOfPage(type);
        if (index >= 0) {
            // Delay the setCurrentItem call to ensure ViewPager is ready
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                viewPager.setCurrentItem(index, false);
                Log.d(TAG, "Page switched to: " + target);
            }, 300); // Adjust delay if needed
        }
    } catch (Exception e) {
        Log.e(TAG, "Invalid target_page: " + target, e);
    }
}

    private void navigateToPageSafely(int index, String target, int attempt) {
        if (isFinishing() || isDestroyed() || viewPager == null) {
            return;
        }

        // Check if ViewPager is ready
        if (viewPager.getAdapter() == null || viewPager.getAdapter().getItemCount() <= index) {
            if (attempt < 3) { // Max 3 attempts
                new Handler(Looper.getMainLooper()).postDelayed(() ->
                        navigateToPageSafely(index, target, attempt + 1), 500);
                return;
            } else {
                Log.e(TAG, "ViewPager not ready after 3 attempts");
                return;
            }
        }

        // Navigate
        viewPager.setCurrentItem(index, false);
        Log.d(TAG, "Page switched to: " + target + " (attempt " + attempt + ")");
    }

//    public void setUpFcm() {
//        // Get FCM token
//        FirebaseMessaging.getInstance().getToken()
//                .addOnCompleteListener(new OnCompleteListener<String>() {
//                    @Override
//                    public void onComplete(@NonNull Task<String> task) {
//                        if (!task.isSuccessful()) {
//                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
//                            return;
//                        }
//
//                        // Get new FCM registration token
//                        String token = task.getResult();
//                        Log.d(TAG, "FCM Token: " + token);
//
//                        // Send token to your server
//                        sendTokenToServer(token);
//                    }
//                });
//
//        // Subscribe to topic (optional)
//        FirebaseMessaging.getInstance().subscribeToTopic("news")
//                .addOnCompleteListener(new OnCompleteListener<Void>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task) {
//                        String msg = "Subscribed to news topic";
//                        if (!task.isSuccessful()) {
//                            msg = "Subscription failed";
//                        }
//                        Log.d(TAG, msg);
//                    }
//                });
//
//        // Handle notification click data
//        if (getIntent().getExtras() != null) {
//            for (String key : getIntent().getExtras().keySet()) {
//                Object value = getIntent().getExtras().get(key);
//                Log.d(TAG, "Key: " + key + " Value: " + value);
//            }
//        }
//    }
public void setUpFcm(String userID) {
    // Get FCM token
    FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);

                    // Send token to your server
                    sendTokenToServer(token);
                }
            });

    // Subscribe to user-specific topic
    FirebaseMessaging.getInstance().subscribeToTopic(userID)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    String msg = "Subscribed to user_1 topic";
                    if (!task.isSuccessful()) {
                        msg = "Subscription to user_1 failed";
                    }
                    Log.d(TAG, msg);
                }
            });

    // Handle notification click data
    if (getIntent().getExtras() != null) {
        for (String key : getIntent().getExtras().keySet()) {
            Object value = getIntent().getExtras().get(key);
            Log.d(TAG, "Key: " + key + " Value: " + value);
        }
    }
}

    // Optional: Subscribe to dynamic user ID
    public void subscribeToUserTopic(String userId) {
        String topic = "user_" + userId;
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "Subscribed to " + topic + " topic";
                        if (!task.isSuccessful()) {
                            msg = "Subscription to " + topic + " failed";
                        }
                        Log.d(TAG, msg);
                    }
                });
    }

    // Optional: Unsubscribe from a topic
    public void unsubscribeFromTopic(String userId) {
        String topic = "user_" + userId;
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "Unsubscribed from " + topic + " topic";
                        if (!task.isSuccessful()) {
                            msg = "Unsubscription from " + topic + " failed";
                        }
                        Log.d(TAG, msg);
                    }
                });
    }
    private void sendTokenToServer(String token) {
        // Implementation to send token to your backend
        Log.d(TAG, "Token sent to server: " + token);
        // Add your server API call here
    }
    public void getFrequencySettings() {
        // Usage example in your activity or service
        Call<FrequencyResponse> call = ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API, DeviceUtils.getIMEI(this)).getFrequencySettings();
        call.enqueue(new Callback<FrequencyResponse>() {
            @SuppressLint("ObsoleteSdkInt")
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(Call<FrequencyResponse> call, Response<FrequencyResponse> response) {
                Log.d(TAG, "getFrequencySettings code=" + response.code() + ", msg=" + response.message());

                if (!response.isSuccessful() || response.body() == null) {
                    Log.w(TAG, "getFrequencySettings: response unsuccessful or body null");
                    return; // âŒ donâ€™t set timer
                }

                List<Frequency> data = response.body().getData();
                if (data == null || data.isEmpty()) {
                    Log.w(TAG, "getFrequencySettings: data is empty, not scheduling timer");
                    return; // âŒ donâ€™t set timer
                }

                // For safety also check index before accessing
                Frequency first = data.get(0);
                if (first == null) {
                    Log.w(TAG, "getFrequencySettings: first item null, not scheduling timer");
                    return;
                }

                Log.d(TAG, "VitalName: " + first.getVitalName());
                Log.d(TAG, "Interval: " + first.getMeasurementInterval() + " " + first.getIntervalUnit());

                // âœ… Only schedule when we actually have valid values
                setTimer(first.getMeasurementInterval(), first.getIntervalUnit());

            }

            @Override
            public void onFailure(Call<FrequencyResponse> call, Throwable t) {
                Log.d(TAG, "getFrequencySettings failed: " + t.toString());
            }
        });

    }

    public void setTimer(int interval, String unit) {
        //int interval = 1;      // e.g., 1 or 2
        //String unit = "min";              // e.g., "min" or "minutes" or "hr"
       // HourlyKickReceiver.setIntervalFromServer(this, interval, unit);
        //Optionally, also one-shot:
        //HourlyKickReceiver.scheduleOneShotIfNeeded(getApplicationContext(), 1, "hour", false);
        //HourlyKickReceiver.scheduleOneShotIfNeeded(this, 15, "minutes", /* force */ false);
//        HourlyKickReceiver.setIntervalFromServer(this, 15, "minutes");
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_IOTSERVICES);
        ContextCompat.registerReceiver(this, dataReceiverBothHeartBloodStep, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);

        // Add navigation receiver
        IntentFilter navigationFilter = new IntentFilter("com.monitor.health.NAVIGATE_TO_PAGE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(navigationReceiver, navigationFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(navigationReceiver, navigationFilter);
        }
        isNavigationReceiverRegistered = true;

    }

    @SuppressLint("WrongConstant")
    private void testBloodPressure() {
        //sample format:
//        {
//            "readings": [
//            {
//                "date": "2025-10-13T08:45:00+08:00",
//                    "device": "66437be266c8833a1c42d7aa",
//                    "manual": false,
//                    "readingType": "5bb306382598931ffbd1b624",
//                    "serial": "jtm00025b94050c",
//                    "timezone": "Asia/Manila",
//                    "value": [120, 80, 72],
//                "source": null
//            },
//            {
//                "date": "2025-10-13T09:00:00+08:00",
//                    "device": "66437be266c8833a1c42d7aa",
//                    "manual": false,
//                    "readingType": "5bb306382598931ffbd1b624",
//                    "serial": "jtm00025b94050c",
//                    "timezone": "Asia/Manila",
//                    "value": [125, 82, 75],
//                "source": null
//            },
//            {
//                "date": "2025-10-13T09:15:00+08:00",
//                    "device": "66437be266c8833a1c42d7aa",
//                    "manual": false,
//                    "readingType": "5bb306382598931ffbd1b624",
//                    "serial": "jtm00025b94050c",
//                    "timezone": "Asia/Manila",
//                    "value": [118, 78, 70],
//                "source": null
//            }
//  ]
//        }
        //HeartRateDao dao = AppDatabase.get(c).heartRateDao();
        BPJumper bpJumper = new BPJumper(23, 24, 25, 1, "1234");
        databaseClient.getAppDatabase().bpJumperDao().insertBPJumper(bpJumper);
        BPJumper bpJumper2 = new BPJumper(26, 27, 28, 1, "1234");
        databaseClient.getAppDatabase().bpJumperDao().insertBPJumper(bpJumper2);
        BPJumper bpJumper3 = new BPJumper(29, 30, 40, 1, "1234");
        databaseClient.getAppDatabase().bpJumperDao().insertBPJumper(bpJumper3);
        BPJumper bpJumper4 = new BPJumper(41, 42, 43, 1, "1234");
        databaseClient.getAppDatabase().bpJumperDao().insertBPJumper(bpJumper4);
        BPJumper bpJumper5 = new BPJumper(44, 45, 46, 1, "1234");
        databaseClient.getAppDatabase().bpJumperDao().insertBPJumper(bpJumper5);
        BPJumper bpJumper6 = new BPJumper(47, 48, 49, 1, "1234");
        databaseClient.getAppDatabase().bpJumperDao().insertBPJumper(bpJumper6);

        BPJumperDao dao = databaseClient.getAppDatabase().bpJumperDao();
        List<BPJumper> heartRateEntities = dao.getAllBPJumper();
        Log.d("Heart-Rate----", String.valueOf(heartRateEntities.size()));

        List<Reading> readingsList = new ArrayList<>();

        for (BPJumper bp : heartRateEntities) {
            Log.d("Heart-Rate----", bp.getSystolic() + "");
            Log.d("Heart-Rate----", bp.getDiastolic() + "");
            Log.d("Heart-Rate----", bp.getPulseRate() + "");
            Log.d("Heart-Rate----", bp.getCreatedAt() + "");

            Reading reading = new Reading();            // NEW object each iteration
            reading.setManual(false);
            reading.setTimezone("Asia/Manila");
            reading.setSource("jtm00025b94050c");
            reading.setValue(Arrays.asList(
                    (double) bp.getSystolic(),
                    (double) bp.getDiastolic(),
                    (double) bp.getPulseRate()
            ));
            reading.setDevice("66437be266c8833a1c42d7aa");
            reading.setReadingType("5bb306382598931ffbd1b624");
            //reading.setDate(DateUtils.getDate());       // or bp.getCreatedAt() if you want per-row time
            reading.setDate(DateUtils.toIso8601Manila(bp.getCreatedAt()));
            reading.setSerial("jtm00025b94050c");

            readingsList.add(reading);                  // append, don't replace
        }

// set once, after the loop
        ReadingsRequest readingsRequest = new ReadingsRequest();
        readingsRequest.setReadings(readingsList);

        Log.d("Heart-Rate--- total", String.valueOf(readingsRequest.getReadings().size()));
        Log.d("Heart-Rate--- first", readingsRequest.getReadings().get(0).toString());
        sendBPJumper(readingsRequest);


    }
    @SuppressLint("WrongConstant")
    private void syncTemperature() {


        TemperatureDao dao = databaseClient.getAppDatabase().temperatureDao();
        List<Temperature> temperatureEntities = dao.getAllTemperature();
        List<Reading> readingsList = new ArrayList<>();

        for (Temperature bp : temperatureEntities) {
            Log.d("temperature----", bp.getTemperature() + "");

            Reading reading = new Reading();            // NEW object each iteration
            reading.setManual(false);
            reading.setTimezone("Asia/Manila");
            reading.setSource("jtm00025b94050c");
            reading.setValue(Arrays.asList(
                    bp.getTemperature()
            ));
            reading.setDevice(Constant.DEVICE_TEMPERATURE);
            reading.setReadingType("5bb306382598931ffbd1b628");
            reading.setDate(DateUtils.toIso8601Manila(bp.getCreatedAt()));
            reading.setSerial("5bc3cb14cba82b066cae7bc1");
            readingsList.add(reading);                  // append, don't replace
        }

// set once, after the loop
        ReadingsRequest readingsRequest = new ReadingsRequest();
        readingsRequest.setReadings(readingsList);
        if(!readingsRequest.getReadings().isEmpty()) {
            sendStepServer(readingsRequest);
        }


    }
    @SuppressLint("WrongConstant")
    private void syncHeartRate() {

        HeartRateDao dao = databaseClient.getAppDatabase().heartRateDao();
        List<HeartRateEntity> heartRateEntities = dao.getAllHeartRate();
        List<Reading> readingsList = new ArrayList<>();

        for (HeartRateEntity bp : heartRateEntities) {

            Reading reading = new Reading();            // NEW object each iteration
            reading.setManual(false);
            reading.setTimezone("Asia/Manila");
            reading.setSource("jtm00025b94050c");
            reading.setValue(Arrays.asList(
                    0.0,
                    bp.getValue()
            ));
            reading.setDevice("66437be266c8833a1c42d7aa");
            reading.setReadingType("5bb306382598931ffbd1b626");
            reading.setDate(DateUtils.toIso8601Manila(bp.getCreatedAt()));
            reading.setSerial("jtm00025b94050c");
            readingsList.add(reading);                  // append, don't replace
        }
        ReadingsRequest readingsRequest = new ReadingsRequest();
        readingsRequest.setReadings(readingsList);
        if(!readingsRequest.getReadings().isEmpty()) {
            sendHeartRateServer(readingsRequest);
        }


    }
    @SuppressLint("WrongConstant")
    private void syncSpo2() {

        OximeterDao dao = databaseClient.getAppDatabase().oximeterDao();
        List<Oximeter> oximeterEntities = dao.getAllOximeterByStatus(1);
        Log.d("Oximeter----", String.valueOf(oximeterEntities.size()));
        List<Reading> readingsList = new ArrayList<>();

        for (Oximeter bp : oximeterEntities) {
            Log.d("Oximeter----", bp.getPulseRate() + "");

            Reading reading = new Reading();            // NEW object each iteration
            reading.setManual(false);
            reading.setTimezone("Asia/Manila");
            reading.setSource(bp.getSerial());
            reading.setValue(Arrays.asList(
                    (double) bp.getPulseRate(),
                    (double) bp.getOxygen()
            ));
            reading.setDevice(Constant.DEVICE_OXIMETER);
            reading.setReadingType("5bb306382598931ffbd1b626");
            reading.setDate(DateUtils.toIso8601Manila(bp.getCreatedAt()));
            reading.setSerial(bp.getSerial());
            readingsList.add(reading);                  // append, don't replace
        }

// set once, after the loop
        ReadingsRequest readingsRequest = new ReadingsRequest();
        readingsRequest.setReadings(readingsList);
        if(!readingsRequest.getReadings().isEmpty()) {
            sendOximeterServer(readingsRequest);
        }


    }

    private void syncBloodPressureData() {

        BPJumperDao dao = databaseClient.getAppDatabase().bpJumperDao();
        List<BPJumper> heartRateEntities = dao.getAllBPJumper();
        List<Reading> readingsList = new ArrayList<>();
        for (BPJumper bp : heartRateEntities) {
            Reading reading = new Reading();            // NEW object each iteration
            reading.setManual(false);
            reading.setTimezone("Asia/Manila");
            reading.setSource("jtm00025b94050c");
            reading.setValue(Arrays.asList(
                    (double) bp.getSystolic(),
                    (double) bp.getDiastolic(),
                    (double) bp.getPulseRate()
            ));
            reading.setDevice("66437be266c8833a1c42d7aa");
            reading.setReadingType("5bb306382598931ffbd1b624");
            //reading.setDate(DateUtils.getDate());       // or bp.getCreatedAt() if you want per-row time
            reading.setDate(DateUtils.toIso8601Manila(bp.getCreatedAt()));
            reading.setSerial("jtm00025b94050c");
            readingsList.add(reading);                  // append, don't replace
        }

// set once, after the loop
        ReadingsRequest readingsRequest = new ReadingsRequest();
        readingsRequest.setReadings(readingsList);
        if(!readingsRequest.getReadings().isEmpty()) {
            sendBPJumper(readingsRequest);
        }


    }

    public void sendBPJumper(ReadingsRequest readingsRequest) {
        Log.d(TAG, "Success : sending the BP");

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,Constant.TOKEN_DR_WATCH_API, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "Bulk Success : "+response);
                Log.d(TAG, "Bulk Success : "+response.code());
                Log.d(TAG, "Bulk Success : "+response.message());
                Log.d(TAG, "Bulk Success : "+response.toString());
                BPJumperDao dao = databaseClient.getAppDatabase().bpJumperDao();
                dao.deleteAll();

            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.d(TAG, "Bulk error : "+t.toString());
            }
        });
    }

    public void sendOximeterServer(ReadingsRequest readingsRequest) {
        Log.d(TAG, "Success : sending the BP");

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,Constant.TOKEN_DR_WATCH_API, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "Bulk Success : "+response);
                Log.d(TAG, "Bulk Success : "+response.code());
                Log.d(TAG, "Bulk Success : "+response.message());
                OximeterDao dao = databaseClient.getAppDatabase().oximeterDao();
                dao.deleteAll();
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.d(TAG, "Bulk error : "+t.toString());
            }
        });
    }

    public void sendHeartRateServer(ReadingsRequest readingsRequest) {
        Log.d(TAG, "Success : sending the BP");

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,Constant.TOKEN_DR_WATCH_API, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "Bulk Success : "+response);
                Log.d(TAG, "Bulk Success : "+response.code());
                Log.d(TAG, "Bulk Success : "+response.message());
                HeartRateDao dao  = databaseClient.getAppDatabase().heartRateDao();
                dao.deleteAll();
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.d(TAG, "Bulk error : "+t.toString());
            }
        });
    }
    public void sendStepServer(ReadingsRequest readingsRequest) {
        Log.d(TAG, "Success : sending the BP");

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,Constant.TOKEN_DR_WATCH_API, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "Bulk Success : "+response);
                Log.d(TAG, "Bulk Success : "+response.code());
                Log.d(TAG, "Bulk Success : "+response.message());
                TemperatureDao dao  = databaseClient.getAppDatabase().temperatureDao();
                dao.deleteAll();
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.d(TAG, "Bulk error : "+t.toString());
            }
        });
    }


//    private final BroadcastReceiver heartRateReceiverOutside = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if ("com.monitor.health.HEART_RATE_UPDATE".equals(intent.getAction())) {
//                float heartRate = intent.getFloatExtra("heartRate", -1f);
//                //Log.d("MainActivity", "Heart rate received: " + heartRate);
//                // Update UI or handle the data here
//                //heartRateText.setText("Heart Rate: " + (int) heartRate + " bpm");
//            }
//        }
//    };

    public void navigateToPage(int position) {
        viewPager.setCurrentItem(position, true); // 'true' for smooth scroll
    }


    public void downloadAndInstallApk(String downloadUrl) {
        Log.d(TAG, "Downloading the task");
        new Thread(() -> {
            try {
                Log.d(TAG, "Downloading the task inside thread");

                URL url = new URL(downloadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                File apkFile = new File(getExternalFilesDir(null), "update.apk");
                boolean isFirst = true;
                try (InputStream in = connection.getInputStream();
                     FileOutputStream out = new FileOutputStream(apkFile)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        if (isFirst) {
                            String head = new String(buffer, 0, bytesRead);
                            Log.d(TAG, "File starts with: " + head);
                            isFirst = false;
                        }
                    }
                }

                Log.d(TAG, "APK file path: " + apkFile.getAbsolutePath());
                Log.d(TAG, "APK file exists: " + apkFile.exists());
                Log.d(TAG, "APK file size: " + apkFile.length() + " bytes");

                Uri apkUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        apkFile
                );

                runOnUiThread(() -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                });

                Log.d(TAG, "Downloading the task Done");

            } catch (Exception e) {
                Log.d(TAG, "Error: " + e.toString());
                e.printStackTrace();
            }
        }).start();
    }

    private void startSensorService() {
        Intent serviceIntent = new Intent(this, StepsService.class);

        // Start service (will run in background)
        startService(serviceIntent);

        // Bind to service for direct communication
        bindService(serviceIntent, serviceConnectionSteps, Context.BIND_AUTO_CREATE);
    }

    private void startHeartRateService() {
        //Intent intent = new Intent(this, HeartRateService.class);
        Intent intent = new Intent(this, HeartRateServiceNative.class);
        //startService(new Intent(this, HeartRateServiceNative.class));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void startOxygenService() {
        //Intent intent = new Intent(this, HeartRateService.class);
//        Intent intent = new Intent(this, BloodOxygenSensorService.class);
//        //startService(new Intent(this, HeartRateServiceNative.class));
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(intent);
//        } else {
//            startService(intent);
//        }
    }

    public void checkVersionDownload() {
        String testAndroidId = "doctorwatchserialtest";
        Log.d(TAG, "Android: " + androidId);

        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);

        Log.d(TAG, "Month: " + month);
        Log.d(TAG, "Year: " + year);

        Call<FileResponseDto> call = ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API, testAndroidId).getFileDownload();
        call.enqueue(new Callback<FileResponseDto>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(Call<FileResponseDto> call, Response<FileResponseDto> response) {
                Log.d(TAG, "File down " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    String filename = response.body().getData().getFilename();
                    Log.d(TAG, "Filename: " + filename);
                    Log.d(TAG, "Filename: link " + response.body().getData().getDownload_url());
//                    prefs.edit().putString("saved_filename", filename).apply();
//                    downloadAndInstallApk(response.body().getData().getDownload_url());
                    // âœ… Check if filename exists in SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                    String savedFilename = prefs.getString("saved_filename", null);

                    Log.d(TAG, "Filename: save" + savedFilename);


                    if (filename.equals(savedFilename)) {
                        Log.d(TAG, "Filename already exists in SharedPreferences. No need to download.");
                    } else {
                        Log.d(TAG, "New filename detected. Proceed with download.");

                        // âœ… Save the new filename
                        prefs.edit().putString("saved_filename", filename).apply();
                        downloadAndInstallApk(response.body().getData().getDownload_url());
                        // TODO: Start the download here
                    }
                } else {
                    Log.d(TAG, "Response not successful.");
                }
            }

            @Override
            public void onFailure(Call<FileResponseDto> call, Throwable t) {
                Log.d(TAG, "Download failed: " + t.toString());
            }
        });
    }

    public void loginDrWatch() {
        //String testAndroidId = "doctorwatchserialtest";
        String testAndroidId = androidId;
        Log.d(TAG, "Android: " + androidId);
        Calendar calendar = Calendar.getInstance();
        // Get month as an integer (1-12)
        int month = calendar.get(Calendar.MONTH) + 1; // Adding 1 because Calendar.MONTH starts from 0
        int year = calendar.get(Calendar.YEAR); // Get the four-digit year

        Log.d(TAG, "Month: " + month);
        Log.d(TAG, "Year: " + year);
        Call<DataObjectDto> call = ApiClient.getUserService(Constant.BASE_URL_BGM,Constant.TOKEN_DR_WATCH_API, testAndroidId).getHealthScore(month, year);
        call.enqueue(new Callback<DataObjectDto>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(Call<DataObjectDto> call, Response<DataObjectDto> response) {
                Log.d(TAG, "loginDrWatch result "+ response.code());
                Log.d(TAG, "loginDrWatch this is the result "+ response.body());
                Log.d(TAG, "loginDrWatch this is the result "+ response.toString());
                Log.d(TAG, "loginDrWatch this is the result "+ response.message());
                if (response.isSuccessful() && response.code() ==200) {
                    // Request successful
                    // Handle response if needed
                    //saveTemperatureData(temperature);

                    assert response.body() != null;
                    //txtUsername.setText(String.format("Username: %s", response.body().getData().username));
                    //txtEmail.setText(String.format("Email: %s", response.body().getData().email));
                    //txtPhone.setText(String.format("Phone: %s", response.body().getData().phone));
                    //txtAddress.setText(String.format("Health Score: %s", response.body().getData().overallHealthScore));


                    Log.d(TAG, "loginDrWatch result "+ response.body());
                    //Log.d(TAG, "loginDrWatch id "+ response.body().getData().getId());
                   if(response.body().getData() != null) {
                        userDrWatch.set_id(response.body().getData().get_id());
                        userDrWatch.setPrescribeTestRate(response.body().getData().getPrescribeTestRate());
                        userDrWatch.setActive(response.body().getData().isActive());
                        userDrWatch.setPhone(response.body().getData().getPhone());
                        userDrWatch.setMemberId(response.body().getData().getMemberId());
                        userDrWatch.setEmail(response.body().getData().getEmail());
                        userDrWatch.setBday(response.body().getData().getBday());
                        userDrWatch.setGender(response.body().getData().getGender());
                        userDrWatch.setBday(response.body().getData().getBday());
                        userDrWatch.setGender(response.body().getData().getGender());
                        userDrWatch.setOrganization(response.body().getData().getOrganization());
                        userDrWatch.setUsername(response.body().getData().getUsername());
                        userDrWatch.setSubOrganization(response.body().getData().getSubOrganization());
                        userDrWatch.setClient(response.body().getData().getClient());
                        userDrWatch.setWithDevice(response.body().getData().isWithDevice());
                        userDrWatch.setPatient_conditions(response.body().getData().getPatient_conditions());
                        userDrWatch.setPractitioners(response.body().getData().getPractitioners());
                        userDrWatch.setAllReadingsFound(response.body().getData().getAllReadingsFound());
                        userDrWatch.setOverallHealthScore(response.body().getData().getOverallHealthScore());
                        databaseClient.getAppDatabase().userDrWatchDao().clearAllUserDrWatch();
                        databaseClient.getAppDatabase().userDrWatchDao().insertUserDrWatch(userDrWatch);

                    }
                    //List<UserDrWatch> list = databaseClient.getAppDatabase().userDrWatchDao().getAllDrWatch();
                    //Log.d(TAG, "loginDrWatch db data "+ list.get(0).fullname);
                    //For testing only
//                    SDK_003_CREATE_UPDATE_USER(
//                            response.body().getData().username,
//                            response.body().getData().email,
//                            response.body().getData().phone,
//                            "FR"
//                    );
                    //sendAlarm();

                } else {
                    // Request failed
                    // Handle error

                }
            }

            @Override
            public void onFailure(Call<DataObjectDto> call, Throwable t) {
                // Request failed
                // Handle failure
                Log.d(TAG, "loginDrWatch result failure"+ t.toString());

            }
        });

    }

    public void userProfile() {
        //String testAndroidId = "doctorwatchserialtest";
        String testAndroidId = androidId;
        Call<UserProfileResponse> call = ApiClient.getUserService(Constant.BASE_URL_BGM,Constant.TOKEN_DR_WATCH_API, testAndroidId).getUserProfile();
        call.enqueue(new Callback<UserProfileResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.code() ==200) {
                   Log.d(TAG, "user profile result "+ response.body().getData());
                   Log.d(TAG, response.body().getData().getAge()+"");
                    setUpFcm(response.body().getData().getId());
                    PreferenceHelper.getInstance(getApplicationContext()).putString(Constant.userHeight, response.body().getData().getHeight());
                    PreferenceHelper.getInstance(getApplicationContext()).putString(Constant.userHeight, response.body().getData().getHeight());
                    PreferenceHelper.getInstance(getApplicationContext()).putInt(Constant.userAge, response.body().getData().getAge());
                    PreferenceHelper.getInstance(getApplicationContext()).putString(Constant.userGender, response.body().getData().getGender());

                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                // Request failed
                // Handle failure
                Log.d(TAG, "loginDrWatch result failure"+ t.toString());

            }
        });

    }

    private void requestAllPermissionsIfNeeded() {
        List<String> needed = new ArrayList<>();

        if (!isPermissionGranted(Manifest.permission.CAMERA))
            needed.add(Manifest.permission.CAMERA);
        if (!isPermissionGranted(Manifest.permission.READ_PHONE_STATE))
            needed.add(Manifest.permission.READ_PHONE_STATE);
        if (!isPermissionGranted(Manifest.permission.BODY_SENSORS))
            needed.add(Manifest.permission.BODY_SENSORS);
        if (!isPermissionGranted(Manifest.permission.BODY_SENSORS_BACKGROUND))
            needed.add(Manifest.permission.BODY_SENSORS_BACKGROUND);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (!isPermissionGranted("android.permission.health.READ_HEART_RATE"))
                needed.add("android.permission.health.READ_HEART_RATE");
            if (!isPermissionGranted("android.permission.health.READ_OXYGEN_SATURATION"))
                needed.add("android.permission.health.READ_OXYGEN_SATURATION");
            if (!isPermissionGranted("android.permission.health.READ_STEPS"))
                needed.add("android.permission.health.READ_STEPS");
        }
        if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION))
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (!isPermissionGranted(Manifest.permission.RECORD_AUDIO))
            needed.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !isPermissionGranted(Manifest.permission.ACTIVITY_RECOGNITION))
            needed.add(Manifest.permission.ACTIVITY_RECOGNITION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!isPermissionGranted(Manifest.permission.BLUETOOTH_SCAN))
                needed.add(Manifest.permission.BLUETOOTH_SCAN);
            if (!isPermissionGranted(Manifest.permission.BLUETOOTH_CONNECT))
                needed.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS))
            needed.add(Manifest.permission.POST_NOTIFICATIONS);

        if (needed.isEmpty()) {
            onAllPermissionsReady();
        } else {
            ActivityCompat.requestPermissions(this,
                    needed.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    private boolean isPermissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void onAllPermissionsReady() {
        startHeartRateService();
        startOxygenService();
        startLocationService();
        startBleService();
        startOxygenSensorService();
    }

    private void startBleService() {
        Intent intent = new Intent(this, BleScanService.class);
//        startService(intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, BleScanService.class));
        } else {
            startService(new Intent(this, BleScanService.class));
        }
        // Bind to the service to call its public methods
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "BLE scan service started");

        // Start sync service alongside BLE so it runs even when app is closed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, MyForegroundService.class));
        } else {
            startService(new Intent(this, MyForegroundService.class));
        }
    }

//    private void startHearRateSensorService() {
//        Intent intent = new Intent(this, HeartRateSensorService.class);
////        startService(intent);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(new Intent(this, HeartRateSensorService.class));
//        } else {
//            startService(new Intent(this, HeartRateSensorService.class));
//        }
//        // Bind to the service to call its public methods
//        //bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
//       // Log.d(TAG, "BLE scan service started");
//
//        startService(intent);
//        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
//    }

@SuppressLint("ObsoleteSdkInt")
private void startHearRateSensorService() {
    Intent intent = new Intent(this, TestService.class);
//        startService(intent);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(new Intent(this, TestService.class));
    } else {
        startService(new Intent(this, TestService.class));
    }
    // Bind to the service to call its public methods
    //bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    // Log.d(TAG, "BLE scan service started");

    startService(intent);
    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
}

    private void startOxygenSensorService() {
//        Intent intent = new Intent(this, BloodOxygenSensorService.class);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(new Intent(this, BloodOxygenSensorService.class));
//        } else {
//            startService(new Intent(this, BloodOxygenSensorService.class));
//        }
//        startService(intent);
//        bindService(intent, mConnectionOxygen, Context.BIND_AUTO_CREATE);
    }

    // Service connection
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            HeartRateSensorService.HeartRateServiceBinder binder =
                    (HeartRateSensorService.HeartRateServiceBinder) service;
            mService = binder.getService();
            mService.startHeartRateMonitoring();
            mBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            mService = null;
        }
    };

    private ServiceConnection mConnectionOxygen = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BloodOxygenSensorService.HeartRateServiceBinder binder =
                    (BloodOxygenSensorService.HeartRateServiceBinder) service;
            mServiceBloodOxygen = binder.getService();
            mServiceBloodOxygen.startOxygenMonitoring();
            mBoundOxygen = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBoundOxygen = false;
            mServiceBloodOxygen = null;
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_PERMISSIONS) return;

        // Build a map of permission â†’ granted for easy lookup
        java.util.Map<String, Boolean> results = new java.util.HashMap<>();
        for (int i = 0; i < permissions.length; i++) {
            results.put(permissions[i], grantResults[i] == PackageManager.PERMISSION_GRANTED);
        }

        // BODY_SENSORS â†’ heart rate + oxygen services
        if (Boolean.TRUE.equals(results.get(Manifest.permission.BODY_SENSORS))) {
            startHeartRateService();
            startOxygenService();
        } else {
            Log.w(TAG, "BODY_SENSORS denied â€” heart rate / oxygen services not started");
        }

        // LOCATION â†’ location service + BLE (pre-Android 12)
        boolean locationGranted = Boolean.TRUE.equals(results.get(Manifest.permission.ACCESS_FINE_LOCATION));
        if (locationGranted) {
            startLocationService();
        } else {
            Log.w(TAG, "ACCESS_FINE_LOCATION denied â€” location service not started");
        }

        // BLE services (Android 12+: need SCAN + CONNECT + LOCATION; pre-12: just LOCATION)
        boolean bleReady;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bleReady = locationGranted
                    && Boolean.TRUE.equals(results.get(Manifest.permission.BLUETOOTH_SCAN))
                    && Boolean.TRUE.equals(results.get(Manifest.permission.BLUETOOTH_CONNECT));
        } else {
            bleReady = locationGranted;
        }
        if (bleReady) {
            startBleService();
            startOxygenSensorService();
        } else {
            Log.w(TAG, "BLE permissions denied â€” BLE scan service not started");
        }
    }

    @SuppressLint("NewApi")
    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        startForegroundService(serviceIntent);
    }
    public void sendAlarm() {
        Log.d(TAG, "SOS "+_model+" : maker "+_maker+" country: "+_country);
        SendAlarmRequest sendAlarmRequest = new SendAlarmRequest(148.752, 87588.701, androidId, 1, 1, batteryPercent, true,
                _model, _maker,
                "0", _country);
        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API, androidId)
                .sendAlarm(sendAlarmRequest);

        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM code " + response.code());
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM body " + response.body());
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM toString " + response.toString());
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM message " + response.message());
                // Hide progress

                // Enable button
               // btnSos.setEnabled(true);
                if (response.code() == 200) {
                    Log.d(TAG, "Alarm sent successfully!");
                    retryCount = 0; // Reset retry count on success
                } else {
                    handleRetry();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                // Hide progress

                // Enable button
               // btnSos.setEnabled(true);
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER Error " + t.toString());
                handleRetry();
            }
        });
    }
    @SuppressLint("LongLogTag")
    private void handleRetry() {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            Log.d(TAG, "Retrying sendAlarm... Attempt " + retryCount);
            sendAlarm();
        } else {
            Log.d(TAG, "Max retry attempts reached. Failed to send alarm.");
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        isInForeground = true;
        Intent intent = new Intent("RESUME_ACTION");
        sendBroadcast(intent);

        //checkVersionDownload();
        Log.d(TAG, "On resume main Reload the data main");

        IntentFilter f = new IntentFilter(ACTION_HEALTH_UPDATE);
        ContextCompat.registerReceiver(this, healthUpdateReceiver, f, ContextCompat.RECEIVER_NOT_EXPORTED);

        // Register broadcast receiver
//        LocalBroadcastManager.getInstance(this).registerReceiver(
//                sensorDataReceiver,
//                new IntentFilter(StepsService.ACTION_SENSOR_DATA)
//        );

        // If service is bound, request data update
        if (serviceBound && sensorService != null) {
            sensorService.requestDataUpdate();
        }

        if (mSensor != null) {
            mSensorManager.registerListener(mHeartRateListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        updateUI();

        IntentFilter filter = new IntentFilter("BLUETOOTH_STATE_CHANGED");
        ContextCompat.registerReceiver(this, bluetoothStateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        //send data to the server
        NetworkUtils.ConnectionQuality connectionQuality =
                NetworkUtils.getConnectionQuality(getApplicationContext());
        if (connectionQuality != NetworkUtils.ConnectionQuality.NONE) {
            syncExecutor.execute(() -> {
                syncBloodPressureData();
                syncSpo2();
                syncHeartRate();
                syncTemperature();
                startFetchingSteps();
            });

        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        isInForeground = false;
        // Key 139 is the back button on this device. Cancel any running SOS timer
        // immediately so it can't fire if the user reopens the app within 5 seconds.
        KeyMonitorAccessibilityService.cancelSosTimer();
         //unregisterReceiver(heartRateReceiverOutside);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(HeartRateServiceNative.ACTION_HEART_RATE);
        ContextCompat.registerReceiver(this, heartRateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    public void saveData(int glucose, int status, String serial){
        Log.d(TAG, "Saving Data --- ");
        ReadingValue readingValue = new ReadingValue();
        readingValue.setEvent(12);
        readingValue.setEventDescription("This is the parentDescription");
        readingValue.setGlucose(glucose);
        readingValue.setStatus(1);
        readingValue.setSerial(serial);
        databaseClient.getAppDatabase().readingValueDao().insertReadingValue(readingValue);

        List<ReadingValue> list = databaseClient.getAppDatabase().readingValueDao().getAllReadingValues();
        Log.d(TAG, "Reading list count : "+list.size());
    }

    public void testData() {
//        BPJumper bpJumper = new BPJumper(12, 34, 80, 1, "12313");
//        databaseClient.getAppDatabase().bpJumperDao().insertBPJumper(bpJumper);

        //WeighingScale weighingScale = new WeighingScale(45, 1, "serial");
        //databaseClient.getAppDatabase().weighingScaleDao().insertWeighingScale(weighingScale);
        //model.setWeightData(45);
    }





    @Override
    public void onSensorDataChanged(int steps, float semaphore, float light, int stepCount) {
       // model.setGlucoseData(steps);
    }


    @SuppressLint("WrongConstant")
    private void initializeSystemServices() {
        systemAssistManager = (HSystemAssistManager) getSystemService("hsystemassist");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (systemAssistManager != null) {
            systemAssistManager.isEnableAccelerate(this);
        } else {
            Log.w(TAG, "HSystemAssistManager unavailable on this device; skipping acceleration init");
        }
    }

    private void setupSensorListener() {
        if (mSensorManager == null) {
            Log.w(TAG, "SensorManager unavailable; skipping heart rate sensor setup");
            return;
        }
        mSensor = mSensorManager.getDefaultSensor(TYPE_HEART_RATE);
        if (mSensor != null) {
            mSensorManager.registerListener(mHeartRateListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Heart rate sensor registered successfully");
        } else {
            Log.e(TAG, "Heart rate sensor not available");
        }
    }



    private void updateUI() {
        runOnUiThread(() -> {
            // Update heart rate display
//            if (tv_heart_rate != null) {
//                tv_heart_rate.setText("Heart Rate: " + heartRateValue + " BPM");
//            }
//            model.setHeartRateMonitor(heartRateValue);
//            model.setOxygen(bloodRateValue);
//            int stepCount = systemAssistManager.getSetpCount();
//            model.setStepCount(stepCount);

            // Update blood rate display
//            if (tv_blood_rate != null) {
//                tv_blood_rate.setText("Blood Rate: " + bloodRateValue);
//            }
//
//            // Update steps display
//            if (txt_steps != null) {
//                try {
//                    int stepCount = systemAssistManager.getSetpCount();
//                    txt_steps.setText("Steps: " + stepCount);
//                } catch (Exception e) {
//                    Log.e(TAG, "Error getting step count: " + e.getMessage());
//                    txt_steps.setText("Steps: --");
//                }
//            }
        });
    }

    private final BroadcastReceiver dataReceiverBothHeartBloodStep = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !intent.hasExtra(ACTION)) {
                Log.d(TAG, "Intent is null or missing action!");
                return;
            }

            int action = intent.getIntExtra(ACTION, -1);
            Log.d(TAG, "Received broadcast action: " + action);

            if (action != -1) {
                switch (action) {
                    case ACTION_HEART:
                        int heart = intent.getIntExtra("heart", 0);
                        Log.d(TAG, "Received heart rate via broadcast: " + heart);
                        heartRateValue = heart;
                        updateUI();
                        break;

                    case ACTION_BLOODO:
                        int bloodo = intent.getIntExtra("blood", 0);
                        Log.d(TAG, "Received blood rate via broadcast: " + bloodo);
                        bloodRateValue = bloodo;
                        updateUI();
                        break;

                    default:
                        Log.d(TAG, "Unknown action received: " + action);
                        break;
                }
            } else {
                Log.d(TAG, "Device disconnected or measurement stopped");
            }
        }
    };

    private void setupAutoScroll() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            int currentPage = 0;
            @Override
            public void run() {
                if (currentPage == pagerAdapter.getItemCount()) {
                    currentPage = 0;
                }
                viewPager.setCurrentItem(currentPage++, true);
                handler.postDelayed(this, 8000); // 3 seconds delay
            }
        };
        // Uncomment to enable auto-scroll
        // handler.postDelayed(runnable, 3000);
    }

    // Show modal dialog
    private void showBluetoothOffDialog() {
       Toast.makeText(this, "Please keep Bluetooth turned on, as turning it off may affect the Dr Watch app.", Toast.LENGTH_LONG).show();
    }

    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + BackButtonInterceptorService.class.getCanonicalName();
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        return enabledServices != null && enabledServices.contains(service);
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        Toast.makeText(this, "Double tap detected!", Toast.LENGTH_SHORT).show();
//        Log.d(TAG, "Keycode: "+ keyCode);
//        Log.d(TAG, "Keycode KEYCODE_HOME: "+ KEYCODE_HOME);
//        if (keyCode == KEYCODE_HOME) {
//            long currentTime = System.currentTimeMillis();
//
//            if (currentTime - lastTapTime <= DOUBLE_TAP_DELAY) {
//                // Double tap detected
//                onHomeButtonDoubleTap();
//                lastTapTime = 0; // reset
//            } else {
//                // First tap
//                lastTapTime = currentTime;
//            }
//            return true; // consume event
//        }
//        return super.onKeyDown(keyCode, event);
//    }
//
//    private void onHomeButtonDoubleTap() {
//        // Your action here
//        Toast.makeText(this, "Double tap detected!", Toast.LENGTH_SHORT).show();
//    }

    public void setViewPagerCurrentItem(int position, boolean smoothScroll) {
        if (viewPager != null) {
            viewPager.setCurrentItem(position, smoothScroll);
        }
    }

    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }

    private void sendBloodPressureRateSync(Integer systolic, Integer diastolic) {
        Log.d(TAG, "Sending BP synchronously: " + systolic + "/" + diastolic);
        try {
            String token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
            Reading reading = new Reading(
                    false,
                    "Asia/Manila",
                    "jtm00025b94050c",
                    Arrays.asList((double) systolic, (double) diastolic, 0.0),
                    "66437be266c8833a1c42d7aa",
                    "5bb306382598931ffbd1b624",
                    getTodayDate(),
                    DeviceUtils.getIMEI(getApplicationContext())
            );
            ReadingsRequest payload = new ReadingsRequest(Arrays.asList(reading));

            ApiClient.getUserService(Constant.BASE_URL_BGM, token, DeviceUtils.getIMEI(getApplicationContext()))
                    .sendReadings(payload)
                    .enqueue(new Callback<Object>() {
                        @Override
                        public void onResponse(Call<Object> call, Response<Object> response) {
                            if (response.isSuccessful()) {
                                Log.d(TAG, "âœ… BP data sent successfully");

                            } else {
                                Log.e(TAG, "âŒ Server error: " + response.code() + " - " + response.message());
                            }
                        }

                        @Override
                        public void onFailure(Call<Object> call, Throwable t) {
                            Log.e(TAG, "âŒ Sync failed", t);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "âŒ Exception during server sync", e);
        }
    }

    private List<PageType> buildPages(TypesAvailability t) {
        List<PageType> pages = new ArrayList<>();

        // Only add those that are TRUE from API:
        if (t.isBloodGlucose()) pages.add(PageType.BLOOD_GLUCOSE);
        if (t.isBloodPressure()) pages.add(PageType.BLOOD_PRESSURE);
        if (t.isWeight()) pages.add(PageType.WEIGHT);
        if (t.isBloodOxygen()) pages.add(PageType.BLOOD_OXYGEN);
        if (t.isElectrocardiogram()) pages.add(PageType.ECG);
        if (t.isTemperature()) pages.add(PageType.TEMPERATURE);

        // These are not part of types_availability JSON (always show if you want):
        pages.add(PageType.HEART_RATE);
        //pages.add(PageType.OXYGEN);
        pages.add(PageType.STEPS);
        //pages.add(PageType.BLOOD_PRESSURE_NATIVE);
        pages.add(PageType.SLEEP);
        pages.add(PageType.PROFILE);

        // Fallback if server says all false:
        if (pages.isEmpty()) {
            pages.add(PageType.PROFILE);
        }

        return pages;
    }

    /** default pages while waiting for API */
    private List<PageType> defaultPages() {
        // show something so ViewPager isn't empty
        return new ArrayList<>(Collections.singletonList(PageType.PROFILE));
    }

    /** find index of a page type in current adapter list */
    private int indexOfPage(PageType type) {
        if (pagerAdapter == null) return -1;
        return pagerAdapter.indexOf(type); // we will add this method in adapter (below)
    }

    public void setVisiblePages(List<PageType> pages) {
        this.visiblePages = pages;
    }

//    public void navigateTo(PageType pageType) {
//
//        int index = visiblePages.indexOf(pageType);
//        Log.d(TAG, "weight this sis the tag value 3 main:"+index);
//        if (index >= 0) {
//            Log.d(TAG, "weight this sis the tag value 3: main inside if");
//            setViewPagerCurrentItem(index, false);
//        }
//    }
    public void navigateTo(PageType pageType) {
        if (pagerAdapter == null) return;

        int index = pagerAdapter.indexOf(pageType);
        Log.d(TAG, "navigateTo " + pageType + " index=" + index);

        if (index >= 0) {
            setViewPagerCurrentItem(index, false);
        }
    }

    private void getAssignDevices() {
        Log.d(TAG, "Running assign data -- ");
        try {
            String token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";

            ApiClient.getUserService(Constant.BASE_URL_BGM, token, DeviceUtils.getIMEI(getApplicationContext()))
                    .getAssignBleDevices()
                    .enqueue(new Callback<DeviceResponseList>() {
                        @Override
                        public void onResponse(Call<DeviceResponseList> call, Response<DeviceResponseList> response) {
                            Log.d(TAG, "âœ… assign data -- ");

                            if (response.isSuccessful() && response.body() != null) {
                                Log.d(TAG, "assign data --" + response.body().toString());

                                // Process the response and sync with database
                                syncDevicesWithDatabase(response.body());
                            } else {
                                Log.e(TAG, "âŒ Server error: " + response.code() + " - " + response.message());
                            }
                        }

                        @Override
                        public void onFailure(Call<DeviceResponseList> call, Throwable t) {
                            Log.e(TAG, "âŒ Sync failed", t);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "âŒ Exception during server sync", e);
            Log.d(TAG, "Error assign data -- ");
        }
    }

    /**
     * Sync devices from API response with local database
     * - Filter devices with assignedSource = "Doctor Watch"
     * - Check if device_id matches known device types
     * - Insert new devices or update isConnected status
     */
    private void syncDevicesWithDatabase(DeviceResponseList responseList) {
        try {
            if (responseList.getData() == null || responseList.getData().isEmpty()) {
                Log.d(TAG, "No devices in API response");
                return;
            }

            DatabaseClient databaseClient = DatabaseClient.getInstance(getApplicationContext());

            // Loop through all devices from API
            for (BLEData bleData : responseList.getData()) {

                String deviceId = bleData.getDeviceId();
                String serial = bleData.getSerial();
                String serverId = bleData.getId();

                // Check if device_id matches known device types
                if (!isKnownDeviceType(deviceId)) {
                   // Log.d(TAG, "Skipping device (unknown type): " + deviceId);
                    continue;
                }



                // Check if device already exists in local database
                BleDeviceModel existingDevice = databaseClient.getAppDatabase()
                        .bleDeviceDao().getByDevice(deviceId);

                //Log.d(TAG, "Processing device exist data: " +existingDevice);

                if (existingDevice == null) {
                    // Device doesn't exist - INSERT new device
                    insertNewDevice(databaseClient, bleData);
                } else {

                    //Log.d(TAG, "Processing device update the data: " + serial + " | DeviceID: " + deviceId+" | Server id:"+ serverId);
                    // Device exists - UPDATE isConnected to true
                    updateDeviceConnected(databaseClient,deviceId, serverId, serial);
                }
            }

            Log.d(TAG, "âœ… Device sync completed");

        } catch (Exception e) {
            Log.e(TAG, "Error syncing devices with database", e);
        }
    }

    /**
     * Check if device_id is one of the known device types from Constant
     */
    private boolean isKnownDeviceType(String deviceId) {
        return deviceId != null && (
                deviceId.equals(Constant.DEVICE_TEMPERATURE) ||      // "5bc3cb14cba82b066cae7bc1"
                        deviceId.equals(Constant.DEVICE_OXIMETER) ||          // "5bc3cb14cba82b066cae7bc2"
                        deviceId.equals(Constant.DEVICE_BP) ||                // "66437be266c8833a1c42d7aa"
                        deviceId.equals(Constant.DEVICE_WEIGHT) ||            // "5d2cac72ed5d7122d4044f0f"
                        deviceId.equals(Constant.DEVICE_GLUCOSE)              // "5be9a0e03d320b73e5f7aa71"
        );
    }

    /**
     * Insert new device into database
     */
    private void insertNewDevice(DatabaseClient databaseClient, BLEData bleData) {
        try {
            BleDeviceModel newDevice = new BleDeviceModel();
            newDevice.setSerial(bleData.getSerial());
            newDevice.setDeviceId(bleData.getDeviceId());
            newDevice.setServerId(bleData.getId());
            // Use device_id to get standardized device name (not from API name)
            newDevice.setDeviceName(getDeviceNameByDeviceId(bleData.getDeviceId()));
            newDevice.setDeviceAddress(bleData.getSerial());
            newDevice.setConnected(true); // Devices from API are connected

            long result = databaseClient.getAppDatabase()
                    .bleDeviceDao().insertIgnore(newDevice);

            Log.d(TAG, "âœ… Inserted new device: " + newDevice.getDeviceName() +
                    " | DeviceID: " + bleData.getDeviceId() +
                    " | Serial: " + bleData.getSerial());

        } catch (Exception e) {
            Log.e(TAG, "Error inserting device", e);
        }
    }

    /**
     * Update existing device - set isConnected to true
     */
    private void updateDeviceConnected(DatabaseClient databaseClient, String deviceId, String serverId, String serial) {
        try {
            int rowsUpdated = databaseClient.getAppDatabase()
                    .bleDeviceDao().updateByDeviceId(deviceId, serverId, true);

//            Log.d(TAG, "âœ… Updated device connection status: " + deviceId +
//                    " | Rows updated: " + rowsUpdated);

        } catch (Exception e) {
            Log.e(TAG, "Error updating device connection status", e);
        }
    }

    /**
     * Extract device name from DeviceDetails
     */
    private String getDeviceNameFromDetails(DeviceDetails deviceDetails) {
        if (deviceDetails != null && deviceDetails.getName() != null) {
            return deviceDetails.getName();
        }
        return "Unknown Device";
    }

    /**
     * Map device_id to readable name (optional helper)
     */
    private String getDeviceTypeName(String deviceId) {
        if (deviceId == null) return "Unknown";

        if (deviceId.equals(Constant.DEVICE_TEMPERATURE)) {
            return "Thermometer";
        } else if (deviceId.equals(Constant.DEVICE_OXIMETER)) {
            return "Oximeter";
        } else if (deviceId.equals(Constant.DEVICE_BP)) {
            return "Blood Pressure";
        } else if (deviceId.equals(Constant.DEVICE_WEIGHT)) {
            return "Weight Scale";
        } else if (deviceId.equals(Constant.DEVICE_GLUCOSE)) {
            return "Glucose Meter";
        }

        return "Unknown";
    }
    //second



    /**
     * Maps device name to device ID using Constant values
     * This function checks if the device_id from API matches known Constant values
     * Returns null if device type is not supported
     */
    private String getDeviceIdByName(String deviceName) {
        if (deviceName == null) {
            return null;
        }

        // Map device names to their corresponding Constant device IDs
        if (deviceName.contains("Thermometer")) {
            return isKnownDevice(Constant.DEVICE_TEMPERATURE) ? Constant.DEVICE_TEMPERATURE : null;
        } else if (deviceName.contains("My Oximeter") || deviceName.contains("Pulse Oximeter")) {
            return isKnownDevice(Constant.DEVICE_OXIMETER) ? Constant.DEVICE_OXIMETER : null;
        } else if (deviceName.contains("BPM") || deviceName.contains("Blood Pressure")) {
            return isKnownDevice(Constant.DEVICE_BP) ? Constant.DEVICE_BP : null;
        } else if (deviceName.contains("Scale") || deviceName.contains("Weight")) {
            return isKnownDevice(Constant.DEVICE_WEIGHT) ? Constant.DEVICE_WEIGHT : null;
        } else if (deviceName.contains("Glucose") || deviceName.contains("EMPECS")) {
            return isKnownDevice(Constant.DEVICE_GLUCOSE) ? Constant.DEVICE_GLUCOSE : null;
        } else if (deviceName.contains("ECG")) {
            return "5bb306382598931ffbd1b627"; // ECG - not in Constant yet
        } else if (deviceName.contains("Activity")) {
            return "5bb306382598931ffbd1b629"; // Activity - not in Constant yet
        }

        return null; // Device type not found
    }

    /**
     * Check if device ID is a known/supported device type
     */
    private boolean isKnownDevice(String deviceId) {
        return deviceId != null && !deviceId.isEmpty();
    }

    /**
     * Alternative: Map device ID directly if you have it from API
     * Use this if you already have the device_id from the API response
     */
    private String getDeviceNameByDeviceId(String deviceId) {
        if (deviceId == null) {
            return null;
        }

        if (deviceId.equals(Constant.DEVICE_TEMPERATURE)) {
            return "My Thermometer";
        } else if (deviceId.equals(Constant.DEVICE_OXIMETER)) {
            return "My Oximeter";
        } else if (deviceId.equals(Constant.DEVICE_BP)) {
            return "JPD BPM";
        } else if (deviceId.equals(Constant.DEVICE_WEIGHT)) {
            return "JPD Scale";
        } else if (deviceId.equals(Constant.DEVICE_GLUCOSE)) {
            return "EMPECS-BBXK010027";
        } else if (deviceId.equals("5bb306382598931ffbd1b627")) {
            return "ECG Monitor";
        } else if (deviceId.equals("5bb306382598931ffbd1b629")) {
            return "Activity Tracker";
        }

        return "Unknown Device";
    }

    /**
     * Check if device_id from API matches any known Constant device
     * Returns the matching Constant value or null
     */
    private String validateDeviceId(String deviceId) {
        if (deviceId == null) {
            return null;
        }

        // Check against all known Constant device IDs
        if (deviceId.equals(Constant.DEVICE_TEMPERATURE) ||
                deviceId.equals(Constant.DEVICE_OXIMETER) ||
                deviceId.equals(Constant.DEVICE_BP) ||
                deviceId.equals(Constant.DEVICE_WEIGHT) ||
                deviceId.equals(Constant.DEVICE_GLUCOSE)) {
            return deviceId; // Valid device ID
        }

        return null; // Device ID not recognized
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ... existing cleanup code ...

        if (isNavigationReceiverRegistered) {
            try {
                unregisterReceiver(navigationReceiver);
                isNavigationReceiverRegistered = false;
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering navigation receiver: " + e.getMessage());
            }
        }
    }

    public void startFetchingSteps(){
        HealthManager.getInstance(this).getSteps(new HealthManager.ValueCallback<Integer>() {
            @Override
            public void onValue(Integer steps) {
                if (steps == null || steps <= 0) return;

                SharedPreferences prefs = getSharedPreferences("steps_prefs", MODE_PRIVATE);
                int lastStepCount = prefs.getInt("last_step_count", 0);

                int delta;
                if (steps < lastStepCount) {
                    // Device restarted â€” step counter was reset to zero
                    delta = steps;
                } else {
                    delta = steps - lastStepCount;
                }

                if (delta > 0) {
                    sendStepsSync(delta, steps, prefs);
                }
            }

            @Override
            public void onError(String error) {

            }
        });
    }

    private void sendStepsSync(int delta, int newStepCount, SharedPreferences prefs) {
        Log.d(TAG, "Sending steps synchronously: " + delta);
        try {
            String token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";

            Reading reading = new Reading(
                    false,
                    "Asia/Manila",
                    "jtm00025b94050c",
                    // API expects a list; here we send [steps]
                    Arrays.asList((double) delta),
                    "66437be266c8833a1c42d7aa",
                    "5bb306382598931ffbd1b629",
                    getTodayDate(),
                    DeviceUtils.getIMEI(this)
            );

            ReadingsRequest payload = new ReadingsRequest(List.of(reading));

            ApiClient.getUserService(Constant.BASE_URL_BGM, token, DeviceUtils.getIMEI(this))
                    .sendReadings(payload)
                    .enqueue(new Callback<Object>() {
                        @Override
                        public void onResponse(Call<Object> call, Response<Object> response) {
                            if (response.isSuccessful()) {
                                prefs.edit().putInt("last_step_count", newStepCount).apply();
                                Log.d(TAG, "âœ… Steps data sent successfully");
                            } else {
                                Log.e(TAG, "âŒ Server returned error: " + response.code() + " - " + response.message() + " â€” steps will retry next sync");
                            }
                        }

                        @Override
                        public void onFailure(Call<Object> call, Throwable t) {
                            Log.e(TAG, "âŒ Sync failed â€” steps will retry next sync", t);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "âŒ Exception during server sync", e);
        }
    }



}
