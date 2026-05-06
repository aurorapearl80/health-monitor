package com.monitor.health.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.monitor.health.FallAlertActivity;
import com.monitor.health.MainActivity;

public class KeyMonitorAccessibilityService extends AccessibilityService {

    private static final String TAG = "Key-change";

    private static final int TARGET_KEY_CODE = 139;
    private static final long LONG_PRESS_MS = 5000L;
    private static final long LAUNCH_COOLDOWN_MS = 10_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Runnable longPressRunnable = null;
    private boolean targetKeyPressed = false;
    private boolean longPressTriggered = false;
    private long lastLaunchTimeMs = 0;

    private String currentForegroundPackage = "";

    // Held so MainActivity can cancel the timer the instant the app goes to background.
    // Key 139 doubles as the back button on this device, so without this the timer
    // would survive the back-press and fire if the user reopened the app within 5 s.
    private static KeyMonitorAccessibilityService instance;

    // Called from MainActivity.onPause() on the main thread — synchronous, no posting needed.
    public static void cancelSosTimer() {
        KeyMonitorAccessibilityService svc = instance;
        if (svc != null) {
            svc.targetKeyPressed = false;
            svc.longPressTriggered = false;
            svc.cancelLongPress();
            Log.d(TAG, "Timer canceled by activity pause");
        }
    }

    @Override
    protected void onServiceConnected() {
        instance = this;
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) info = new AccessibilityServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        setServiceInfo(info);
        Log.d(TAG, "connected");
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        // IMPORTANT: always return false — never consume the event.
        // Returning true would swallow the back-navigation so the app never exits,
        // leaving the 5-second timer running until it fires and shows the dialog.
        // Back navigation is the system's job; our job is only to observe.

        if (event.getKeyCode() != TARGET_KEY_CODE) {
            if (targetKeyPressed && !longPressTriggered) {
                Log.d(TAG, "Different key pressed — canceling SOS timer");
                targetKeyPressed = false;
                cancelLongPress();
            }
            return false;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            if (!targetKeyPressed && MainActivity.isInForeground) {
                targetKeyPressed = true;
                longPressTriggered = false;
                scheduleLongPress();
                Log.d(TAG, "SOS timer started");
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            if (targetKeyPressed) {
                targetKeyPressed = false;
                cancelLongPress();
                longPressTriggered = false;
                Log.d(TAG, "Key released — SOS timer canceled");
            }
        }

        return false; // never consume — let system handle back navigation
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = event.getPackageName();
            if (pkg != null && !pkg.toString().isEmpty()) {
                currentForegroundPackage = pkg.toString();
            }

            // Cancel timer if the user navigated away from our app while holding the key
            if (targetKeyPressed && !longPressTriggered && !MainActivity.isInForeground) {
                Log.d(TAG, "App left foreground during key hold — canceling SOS timer");
                targetKeyPressed = false;
                cancelLongPress();
            }
        }
    }

    private void scheduleLongPress() {
        cancelLongPress();
        longPressRunnable = () -> {
            if (!targetKeyPressed) return;

            // Final check at fire time: if the app left the foreground while the timer
            // was running (e.g., user pressed back), abort. Uses the activity-lifecycle
            // flag which is more reliable than accessibility window-state events.
            if (!MainActivity.isInForeground) {
                Log.d(TAG, "App left foreground before timer fired — canceling SOS");
                targetKeyPressed = false;
                return;
            }

            longPressTriggered = true;
            targetKeyPressed = false;
            Log.d(TAG, "Long-press confirmed — launching FallAlertActivity");
            launchFallAlertActivity();
        };
        handler.postDelayed(longPressRunnable, LONG_PRESS_MS);
    }

    private void cancelLongPress() {
        if (longPressRunnable != null) {
            handler.removeCallbacks(longPressRunnable);
            longPressRunnable = null;
        }
    }

    private void launchFallAlertActivity() {
        if (FallAlertActivity.isShowing) {
            Log.d(TAG, "Ignoring launch — dialog already visible");
            return;
        }
        if (!MainActivity.isInForeground) {
            Log.d(TAG, "Ignoring launch — app not in foreground");
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastLaunchTimeMs < LAUNCH_COOLDOWN_MS) {
            Log.d(TAG, "Ignoring launch — within cooldown window");
            return;
        }
        lastLaunchTimeMs = now;
        Intent i = new Intent(this, FallAlertActivity.class);
        i.putExtra("sos_triggered", true);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        instance = null;
        targetKeyPressed = false;
        longPressTriggered = false;
        cancelLongPress();
        super.onDestroy();
    }
}
