package com.monitor.health.utility;


import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LauncherItems {
    public final static String HEART_ACTION = "com.hsciot.healthy.action.start";
    public final static String BLOOD_ACTION = "com.hsciot.blood.action.start";

    public final static String HEART_PKG = "com.hsc.heartrate";

    public final static long DELAY_BLOOD_RESULTS = 1000 * 58;//è¶…æ—¶æ—¶é—´1åˆ†é’Ÿ
    public final static long DELAY_HEART_RESULTS = 1000 * 40;


    public final static int ACTION_HEART = 1 << 11;
    public final static int ACTION_BLOODO = 102343;


    public final static int ACTION_HEART_CLASE = 102347;
    public final static int ACTION_BLOOD_CLASE = 102348;

    public static void startHeart(Context context) {
        Intent it = new Intent(HEART_ACTION);
        it.setPackage(HEART_PKG);
        it.putExtra("action", ACTION_HEART);
        //å¯è®¾ç½®è¿è¡Œæ—¶é—´
        it.putExtra("time", DELAY_HEART_RESULTS);
        context.startService(it);
    }

    public static void stopHeart(Context context) {
        Intent it = new Intent(HEART_ACTION);
        it.setPackage(HEART_PKG);
        context.stopService(it);
    }


    public static void startBlood(Context context) {
        Intent it = new Intent(BLOOD_ACTION);
        it.setPackage(HEART_PKG);
        it.putExtra("action", ACTION_HEART);
        it.putExtra("count", 1);
        //å¯è®¾ç½®è¿è¡Œæ—¶é—´
        it.putExtra("time", DELAY_BLOOD_RESULTS);
        context.startService(it);
    }

    public static void stopBlood(Context context) {
        Intent it = new Intent(BLOOD_ACTION);
        it.setPackage(HEART_PKG);
        context.stopService(it);
    }
}

