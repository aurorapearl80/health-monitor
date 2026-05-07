package com.monitor.health;

public class Constant {
    //Wifi credential
    public static final String NETWORK_SSID = "PLDTHOMEFIBR9Qr7U";
    public static final String NETWORK_PASS = "MEmyselfandIan@1986";
    //Login credential
    public static final String EMAIL = "edianon";
    public static final String PASSWORD = "password";

    public static final String BASE_URL = "https://api.smarthealth1on1.com/";
    public static final String BASE_URL_BGM = "https://doctorwatch.smarthealth1on1.com/";
    //public static final String BASE_URL_FALL_DETECTION = "https://preprod.drsecurityapp.com:8079/appservices/webservices/";
    public static final String BASE_URL_FALL_DETECTION = "https://lbaws.drsecurity.es/appservices/webservices/";


    public static final String SERVICES_UUID = "00001808-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC_UUID = "00002A18-0000-1000-8000-00805f9b34fb";
    public static final String DESCRIPTION_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    public static final String THERMOMETER_SERVICE_UUID = "cdeacb80-5235-4c07-8846-93a37ee6b86d";
    public static final String THERMOMETER_CHARACTER_UUID = "cdeacb81-5235-4c07-8846-93a37ee6b86d";

    public static final String JPD_BPM_SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static final String JPD_BPM_CHARACTER_UUID = "0000fff1-0000-1000-8000-00805f9b34fb";

    public static final String PULSE_OXIMETER_SERVICE_UUID = "cdeacb80-5235-4c07-8846-93a37ee6b86d";
    public static final String PULSE_OXIMETER_CHARACTER_UUID = "cdeacb81-5235-4c07-8846-93a37ee6b86d";

    public static final String PULSE_PILL_DISPENSER_SERVICE_UUID = "00001800-0000-1000-8000-00805f9b34fb";
    //public static final String PULSE_PILL_DISPENSER_SERVICE_UUID = "2F2DFFF0-2E85-649D-3545-3586428F5DA3";
    //public static final String PULSE_PILL_DISPENSER_CHARACTER_UUID = "2F2DFFF4-2E85-649D-3545-3586428F5DA3";
    public static final String PULSE_PILL_DISPENSER_CHARACTER_UUID = "00002a00-0000-1000-8000-00805f9b34fb";
    public static final String PULSE_PILL_DESCRIPTION_UUID = "2F2DFFF0-2E85-649D-3545-3586428F5DA3";

    public static final String TOKEN = "4b81be11b6f717d8a7e46197386b792446d4ab3f293b8ca33e3ae1ab01b9b47f";

    public static final String TOKEN_DR_WATCH_API = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";

    //Send Alarm to DRS Api Smarth Health
    public static final String UUID_TOKEN = "c4e471c9-64a8-4cb7-bc57-01676752ab09";

    //Blood pressure
    public static final String ACTION_BLOOD_PRESSURE = "ACTION_BLOOD_PRESSURE";
    public static final String VALUE_BLOOD_PRESSURE = "VALUE_BLOOD_PRESSURE";
    public static final String PREFERENCE_BLOOD_PRESSURE = "PREFERENCE_BLOOD_PRESSURE";
    public static final String PREFERENCE_BPM = "PREFERENCE_BPM";

    public static final String ACTION_BLOOD_GLUCOSE = "ACTION_BLOOD_GLUCOSE";
    public static final String VALUE_BLOOD_GLUCOSE = "VALUE_BLOOD_GLUCOSE";
    public static final String VALUE_BLOOD_GLUCOSE_MAIL_VALUE = "VALUE_BLOOD_GLUCOSE_MAIL_VALUE";
    public static final String VALUE_BLOOD_GLUCOSE_UNIT_VALUE = "VALUE_BLOOD_GLUCOSE_UNIT_VALUE";
    public static final String PREFERENCE_BLOOD_GLUCOSE = "PREFERENCE_BLOOD_GLUCOSE";
    public static final String PREFERENCE_VALUE_BLOOD_GLUCOSE = "PREFERENCE_VALUE_BLOOD_GLUCOSE";
    public static final String PREFERENCE_IME = "PREFERENCE_IME";
   // public static final String PREFERENCE_VALUE_IME = "PREFERENCE_VALUE_IME";

    public static final String ACTION_TEMPERATURE = "ACTION_TEMPERATURE";
    public static final String VALUE_TEMPERATURE = "VALUE_TEMPERATURE";

    public static final String ACTION_PULSE_OXIMETER = "ACTION_PULSE_OXIMETER";
    public static final String VALUE_PULSE_OXIMETER_PULSE_RATE = "VALUE_PULSE_OXIMETER_PULSE_RATE";
    public static final String VALUE_OXIMETER_PULSE_OXYGEN = "VALUE_OXIMETER_PULSE_OXYGEN";


    public static final String ACTION_WEIGHT = "ACTION_WEIGHT";
    public static final String VALUE_WEIGHT = "VALUE_WEIGHT";

    public static final String ACTION_HEART_RATE_DATA = "com.monitor.health.HEART_RATE_DATA";
    public static final String ACTION_SENSOR_ERROR = "com.monitor.health.SENSOR_ERROR";
    public static final String EXTRA_HEART_RATE = "heart_rate";
    public static final String EXTRA_ERROR_MESSAGE = "error_message";
    public static final String EXTRA_SENSOR_MODE = "sensor_mode";

    public static final String ACTION_BLOOD_OXYGEN_DATA = "com.monitor.health.BLOOD_OXYGEN_DATA";
    public static final String EXTRA_BLOOD_OXYGEN_VALUE = "blood_oxygen";
    public static final String ACTION_HEART_RATE_MONITOR_FROM_JAR = "ACTION_HEART_RATE_MONITOR_FROM_JAR";
    public static final String VALUE_HEART_RATE_MONITOR_FROM_JAR = "VALUE_HEART_RATE_MONITOR_FROM_JAR";

    public static final String ACTION_HEALTH_UPDATE = "com.drwatch.HEALTH_UPDATE";

    public static final String ACTION_KICK = "com.monitor.health.ACTION_KICK_HOURLY";

    public static final String ID_ACTIVITY = "5bb306382598931ffbd1b629";
    public static final String ID_BLOOD_GLUCOSE = "5bb306382598931ffbd1b623";
    public static final String ID_BLOOD_OXYGEN = "5bb306382598931ffbd1b626";
    public static final String ID_BLOOD_PRESSURE = "5bb306382598931ffbd1b624";
    public static final String ID_ECG = "5bb306382598931ffbd1b627";
    public static final String ID_TEMPERATURE = "5bb306382598931ffbd1b628";
    public static final String ID_WEIGHT = "5bb306382598931ffbd1b625";

    //auth
    public static final String AUTH_TOKEN = "auth_token";

    //preferences
    public static final String weightUnit = "weightUnit";
    public static final String weightUnitBoolean = "weightUnitBoolean";
    //User profile
    public static final String userHeight = "userHeight";
    public static final String userAge = "userAge";
    public static final String userGender = "userGender";

    //Title
    public static final String BLOOD_PRESSURE = "Blood Pressure";
    public static final String BLOOD_GLUCOSE = "Blood Glucose";
    public static final String BLOOD_OXYGEN = "Blood Oxygen";
    public static final String WEIGHT = "Weight";
    public static final String TEMPERATURE = "Temperature";
    public static final String TITLE_STEPS = "TESTING_STEP";
    public static final String CELSIUS_VALUE = "\u2103"; // â„ƒ
    public static final String FAHRENHEIT_VALUE = "\u2109"; // â„‰
    public static final String FAHRENHEIT_TEXT = "FAHRENHEIT"; // â„‰
    public static final String CELSIUS_TEXT = "CELSIUS"; // â„‰
    public static final String APP_VERSION = "Version: v1.1.16"; // â„‰

    public static final String DEVICE_TEMPERATURE = "5bc3cb14cba82b066cae7bc1";
    public static final String DEVICE_OXIMETER = "5bc3cb14cba82b066cae7bc2";
    public static final String DEVICE_BP = "66437be266c8833a1c42d7aa";
    public static final String DEVICE_WEIGHT = "5d2cac72ed5d7122d4044f0f";
    public static final String DEVICE_GLUCOSE = "5e4c0db6bc20236a64ca3467";





}
