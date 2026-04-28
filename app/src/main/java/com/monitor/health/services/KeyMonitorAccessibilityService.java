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

public class KeyMonitorAccessibilityService extends AccessibilityService {

    private static final String TAG = "Key-change";

    private static final int TARGET_KEY_CODE = 139;
    private static final long LONG_PRESS_MS = 3000L;
    private static final long LAUNCH_COOLDOWN_MS = 10_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Runnable longPressRunnable = null;
    private boolean targetKeyPressed = false;
    private boolean longPressTriggered = false;
    private long lastLaunchTimeMs = 0;

    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) info = new AccessibilityServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        setServiceInfo(info);
        Log.d(TAG, "connected");
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != TARGET_KEY_CODE) {
            if (targetKeyPressed && !longPressTriggered
                    && event.getAction() == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "Different key pressed while waiting for SOS long-press, canceling");
                targetKeyPressed = false;
                cancelLongPress();
            }
            return false;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (!targetKeyPressed && event.getRepeatCount() == 0) {
                targetKeyPressed = true;
                longPressTriggered = false;
                scheduleLongPress();
            }
            return false;
        }

        if (event.getAction() != KeyEvent.ACTION_UP) return false;

        targetKeyPressed = false;
        cancelLongPress();

        if (longPressTriggered) {
            longPressTriggered = false;
            return true;
        }

        return false;
    }

    private void scheduleLongPress() {
        cancelLongPress();
        longPressRunnable = () -> {
            if (!targetKeyPressed) {
                return;
            }
            longPressTriggered = true;
            targetKeyPressed = false;
            Log.d(TAG, "Long-press detected, launching FallAlertActivity");
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
            Log.d(TAG, "Ignoring launch â€” dialog already visible");
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastLaunchTimeMs < LAUNCH_COOLDOWN_MS) {
            Log.d(TAG, "Ignoring launch â€” within cooldown window");
            return;
        }
        lastLaunchTimeMs = now;
        Intent i = new Intent(this, FallAlertActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        targetKeyPressed = false;
        longPressTriggered = false;
        cancelLongPress();
        super.onDestroy();
    }
}
