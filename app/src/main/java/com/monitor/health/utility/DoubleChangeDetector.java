package com.monitor.health.utility;
import android.os.Handler;
import android.util.Log;

public class DoubleChangeDetector {

    private double currentInt = 0;
    private double lastInt = 0;
    private final int DELAY = 1000; // Delay in milliseconds
    private Handler handler = new Handler();
    private int oxygen = 0;
    private OnDoubleChangeListener onDoubleChangeListener;

    public interface OnDoubleChangeListener {
        void onDoubleChange(double newInt);
        void onDoubleStable(double stableInt);
    }

    public void setOnDoubleChangeListener(OnDoubleChangeListener listener) {
        this.onDoubleChangeListener = listener;
    }

    public void updateDouble(double newInt) {
        Log.d("IntChangeDetector", "updateInt called with: " + newInt);

        // Cancel previous checks
        handler.removeCallbacks(checkForUnchangedInt);

        // Update the current int
        currentInt = newInt;

        // Notify the listener about the int change
        if (onDoubleChangeListener != null) {
            onDoubleChangeListener.onDoubleChange(currentInt);
        }

        // Schedule a new check
        handler.postDelayed(checkForUnchangedInt, DELAY);
    }

    private final Runnable checkForUnchangedInt = new Runnable() {
        @Override
        public void run() {
            //Log.d("IntChangeDetector", "Runnable executed. Current: " + currentInt + ", LastStable: " + lastInt);

            lastInt = currentInt; // <-- add this

            if (onDoubleChangeListener != null) {
                onDoubleChangeListener.onDoubleStable(currentInt);
            }
        }
    };

}
