package com.monitor.health.utility;

import android.content.Context;
import android.content.SharedPreferences;

import com.monitor.health.Constant;
import com.monitor.health.response.ble.MeasurementUnit;
import com.monitor.health.response.ble.MeasurementUnits;

public class PreferenceHelper {

    private static final String PREF_NAME = "preference_helper";
    private static PreferenceHelper instance;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    // Private constructor (singleton pattern)
    private PreferenceHelper(Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Singleton instance
    public static synchronized PreferenceHelper getInstance(Context context) {
        if (instance == null) {
            instance = new PreferenceHelper(context);
        }
        return instance;
    }

    // Save string
    public void putString(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    // Get string
    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    // Save int
    public void putInt(String key, int value) {
        editor.putInt(key, value);
        editor.apply();
    }

    // Get int
    public int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    // Save boolean
    public void putBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    // Get boolean
    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    // Remove single key
    public void remove(String key) {
        editor.remove(key);
        editor.apply();
    }

    // Save all measurement units from the API response object
    public void saveMeasurementUnits(MeasurementUnits units) {
        if (units == null) return;
        saveMeasurementUnit(Constant.PREF_UNIT_HEIGHT_VALUE,      Constant.PREF_UNIT_HEIGHT_LABEL,      units.getUnitHeight());
        saveMeasurementUnit(Constant.PREF_UNIT_WEIGHT_VALUE,      Constant.PREF_UNIT_WEIGHT_LABEL,      units.getUnitWeight());
        saveMeasurementUnit(Constant.PREF_UNIT_TEMPERATURE_VALUE, Constant.PREF_UNIT_TEMPERATURE_LABEL, units.getUnitTemperature());
        saveMeasurementUnit(Constant.PREF_UNIT_GLUCOSE_VALUE,     Constant.PREF_UNIT_GLUCOSE_LABEL,     units.getUnitGlucose());
        saveMeasurementUnit(Constant.PREF_UNIT_BP_VALUE,          Constant.PREF_UNIT_BP_LABEL,          units.getUnitBloodPressure());
    }

    private void saveMeasurementUnit(String valueKey, String labelKey, MeasurementUnit unit) {
        if (unit == null) return;
        editor.putString(valueKey, unit.getValue());
        editor.putString(labelKey, unit.getLabel());
        editor.apply();
    }

    // Read helpers — any screen can call these directly
    public String getHeightUnit()      { return sharedPreferences.getString(Constant.PREF_UNIT_HEIGHT_VALUE, "centimeters"); }
    public String getHeightLabel()     { return sharedPreferences.getString(Constant.PREF_UNIT_HEIGHT_LABEL, "Centimeters"); }

    public String getWeightUnit()      { return sharedPreferences.getString(Constant.PREF_UNIT_WEIGHT_VALUE, "pounds"); }
    public String getWeightLabel()     { return sharedPreferences.getString(Constant.PREF_UNIT_WEIGHT_LABEL, "Pounds"); }

    public String getTemperatureUnit() { return sharedPreferences.getString(Constant.PREF_UNIT_TEMPERATURE_VALUE, "celsius"); }
    public String getTemperatureLabel(){ return sharedPreferences.getString(Constant.PREF_UNIT_TEMPERATURE_LABEL, "Celsius"); }

    public String getGlucoseUnit()     { return sharedPreferences.getString(Constant.PREF_UNIT_GLUCOSE_VALUE, "mg_dl"); }
    public String getGlucoseLabel()    { return sharedPreferences.getString(Constant.PREF_UNIT_GLUCOSE_LABEL, "mg/dL"); }

    public String getBloodPressureUnit()  { return sharedPreferences.getString(Constant.PREF_UNIT_BP_VALUE, "mmhg"); }
    public String getBloodPressureLabel() { return sharedPreferences.getString(Constant.PREF_UNIT_BP_LABEL, "mmHg"); }
}
