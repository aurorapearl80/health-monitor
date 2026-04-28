package com.monitor.health.services;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.monitor.health.MainActivity;

public class BackButtonInterceptorService extends AccessibilityService {
    private static final long LONG_PRESS_THRESHOLD = 1000; // 1 second
    private long backPressStartTime = 0;
    private boolean isBackPressed = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Handle accessibility events if needed
    }

    @Override
    public void onInterrupt() {
        // Handle interruption
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        Log.d("Back------------------",  event.getKeyCode()+"");
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                backPressStartTime = System.currentTimeMillis();
                isBackPressed = true;
                return true; // Consume the event
            } else if (event.getAction() == KeyEvent.ACTION_UP && isBackPressed) {
                isBackPressed = false;
                long pressDuration = System.currentTimeMillis() - backPressStartTime;

                if (pressDuration >= LONG_PRESS_THRESHOLD) {
                    handleBackButtonLongPress();
                    return true; // Consume the event
                } else {
                    // Let system handle short press
                    return false;
                }
            }
        }
        return super.onKeyEvent(event);
    }

    private void handleBackButtonLongPress() {
        // Your custom logic here
        // For example, show a notification or start an activity
        showNotification("Back button long pressed!");

        // Or start a specific activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void showNotification(String message) {
        Log.d("Back------------------",  message);
    }
}
