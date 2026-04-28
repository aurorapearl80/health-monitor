package com.monitor.health.utility;
import android.os.Handler;
import android.util.Log;

public class IntChangeDetector {

    private int currentInt = 0;
    private int lastInt = 0;
    private final int DELAY = 1000; // Delay in milliseconds
    private Handler handler = new Handler();
    private int oxygen = 0;
    private OnIntChangeListener onIntChangeListener;

    public interface OnIntChangeListener {
        void onIntChange(int newInt, int oxygen);
        void onIntStable(int stableInt, int oxygen);
    }

    public void setOnIntChangeListener(OnIntChangeListener listener) {
        this.onIntChangeListener = listener;
    }

    public void updateInt(int newInt, int _oxygen) {
        //Log.d("IntChangeDetector", "updateInt called with: " + newInt);

        // Cancel previous checks
        handler.removeCallbacks(checkForUnchangedInt);

        // Update the current int
        currentInt = newInt;
        oxygen = _oxygen;

        // Notify the listener about the int change
        if (onIntChangeListener != null) {
            onIntChangeListener.onIntChange(currentInt, oxygen);
        }

        // Schedule a new check
        handler.postDelayed(checkForUnchangedInt, DELAY);
    }

    private final Runnable checkForUnchangedInt = new Runnable() {
        @Override
        public void run() {
            Log.d("IntChangeDetector", "Runnable executed. Current int: " + currentInt + ", Last int: " + lastInt);
            onIntChangeListener.onIntStable(currentInt, oxygen);
//            if (currentInt == lastInt) {
//                // Int has not changed during the delay period
//                if (onIntChangeListener != null) {
//                    onIntChangeListener.onIntStable(currentInt);
//                }
//            } else {
//                // Int has changed, update lastInt
//                lastInt = currentInt;
//            }
        }
    };
}
