package com.monitor.health.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyWatcherService extends AccessibilityService {
    private static final String TAG = "KeyWatcher";
    private static final long LONG_PRESS_MS = 600;

    // Keys we care about (HOME is often blocked by the OS; STEM_* may work on Wear)
    private static final int[] WATCH_KEYS = new int[] {
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_STEM_PRIMARY,
            KeyEvent.KEYCODE_STEM_1,
            KeyEvent.KEYCODE_STEM_2,
            KeyEvent.KEYCODE_STEM_3
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SparseArray<Long> downAt = new SparseArray<>();
    private final SparseArray<Boolean> longFired = new SparseArray<>();
    private final SparseArray<Runnable> pending = new SparseArray<>();

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "onServiceConnected");

        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) info = new AccessibilityServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        setServiceInfo(info);

        // IMPORTANT: actually request key filtering
        //requestFilterKeyEvents(true);
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        if (!isWatchKey(keyCode)) return super.onKeyEvent(event);

        final int action = event.getAction();
        final long now = SystemClock.uptimeMillis();

        Log.d(TAG, "key=" + keyCode + " " + KeyEvent.keyCodeToString(keyCode)
                + " sc=" + event.getScanCode()
                + " action=" + (action == KeyEvent.ACTION_DOWN ? "DOWN" : "UP")
                + " rep=" + event.getRepeatCount());

        if (action == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            // First DOWN â†’ arm long-press timer
            downAt.put(keyCode, now);
            longFired.put(keyCode, false);

            Runnable r = () -> {
                Boolean fired = longFired.get(keyCode);
                if (fired != null && !fired) {
                    longFired.put(keyCode, true);
                    onKeyLongPress(keyCode);
                }
            };
            pending.put(keyCode, r);
            handler.postDelayed(r, LONG_PRESS_MS);
            return false; // donâ€™t block system behavior
        }

        if (action == KeyEvent.ACTION_UP) {
            // Cancel timer
            Runnable r = pending.get(keyCode);
            if (r != null) handler.removeCallbacks(r);
            pending.remove(keyCode);

            boolean fired = Boolean.TRUE.equals(longFired.get(keyCode));
            Long started = downAt.get(keyCode);
            downAt.remove(keyCode);
            longFired.remove(keyCode);

            if (!fired && started != null) {
                long held = now - started;
                // This was a short press; do nothing or handle a "tap" here if you want.
                Log.d(TAG, "Short press (" + held + "ms)");
            }
            return false;
        }

        // Ignore repeats; timer handles long press
        return false;
    }

    private boolean isWatchKey(int keyCode) {
        for (int k : WATCH_KEYS) if (k == keyCode) return true;
        return false;
    }

    private void onKeyLongPress(int keyCode) {
        Log.d(TAG, "LONG PRESS: " + KeyEvent.keyCodeToString(keyCode));
        // TODO: your action here (e.g., launch an activity)
        // Intent i = new Intent(this, MainActivity.class);
        // i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // startActivity(i);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {}
    @Override public void onInterrupt() {}
}
