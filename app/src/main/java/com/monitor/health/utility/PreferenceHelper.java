package com.monitor.health.utility;

import android.content.Context;
import android.content.SharedPreferences;

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
}
