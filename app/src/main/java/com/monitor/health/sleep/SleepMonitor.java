package com.monitor.health.sleep;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class SleepMonitor {

    private static final String TAG = "SleepMonitor";

    // Sleep detection thresholds
    private static final double SLEEP_HR_THRESHOLD_FACTOR = 0.85; // 85% of baseline HR
    private static final double MOVEMENT_THRESHOLD = 2.0; // m/sÂ² for movement detection
    private static final long SLEEP_DETECTION_WINDOW = 300000; // 5 minutes in milliseconds
    private static final long DEEP_SLEEP_WINDOW = 900000; // 15 minutes for deep sleep

    // Data storage
    private List<SleepDataPoint> sleepData;
    private SleepSession currentSession;

    // Current values
    private int currentHeartRate = 0;
    private double currentMovementLevel = 0.0;
    private long lastMovementTime = 0;

    // Sleep state tracking
    private SleepState currentSleepState = SleepState.AWAKE;
    private long sleepStateStartTime = 0;

    // User baseline values
    private double baselineHeartRate = 70.0;
    private boolean isMonitoring = false;

    public enum SleepState {
        AWAKE,
        LIGHT_SLEEP,
        DEEP_SLEEP,
        REM_SLEEP
    }

    public static class SleepDataPoint {
        public final long timestamp;
        public final int heartRate;
        public final double movementLevel;
        public final SleepState sleepState;

        public SleepDataPoint(long timestamp, int heartRate, double movementLevel, SleepState sleepState) {
            this.timestamp = timestamp;
            this.heartRate = heartRate;
            this.movementLevel = movementLevel;
            this.sleepState = sleepState;
        }

        @Override
        public String toString() {
            return String.format("Sleep Data: %s - HR:%d, Movement:%.2f, State:%s",
                    new Date(timestamp).toString(), heartRate, movementLevel, sleepState);
        }
    }

    public static class SleepSession {
        public long sleepStartTime;
        public long sleepEndTime;
        public long totalSleepDuration;
        public long deepSleepDuration;
        public long lightSleepDuration;
        public long remSleepDuration;
        public int averageHeartRate;
        public int sleepEfficiency; // Percentage
        public int awakePeriods;

        public SleepSession() {
            this.sleepStartTime = System.currentTimeMillis();
        }

        @NonNull
        @SuppressLint("DefaultLocale")
        @Override
        public String toString() {
            return String.format("Sleep Session: %d hours %d minutes\n" +
                            "Deep: %d min, Light: %d min, REM: %d min\n" +
                            "Avg HR: %d, Efficiency: %d%%, Awake periods: %d",
                    totalSleepDuration / (1000 * 60 * 60),
                    (totalSleepDuration % (1000 * 60 * 60)) / (1000 * 60),
                    deepSleepDuration / (1000 * 60),
                    lightSleepDuration / (1000 * 60),
                    remSleepDuration / (1000 * 60),
                    averageHeartRate, sleepEfficiency, awakePeriods);
        }
    }

    public SleepMonitor() {
        this.sleepData = new ArrayList<>();
    }

    // Constructor with custom baseline heart rate
    public SleepMonitor(double baselineHeartRate) {
        this();
        this.baselineHeartRate = baselineHeartRate;
    }

    public void startSleepMonitoring() {
        if (isMonitoring) {
            Log.w(TAG, "Sleep monitoring already started");
            return;
        }

        isMonitoring = true;
        currentSession = new SleepSession();
        sleepData.clear();
        currentSleepState = SleepState.AWAKE;
        sleepStateStartTime = System.currentTimeMillis();

        Log.d(TAG, "Sleep monitoring started");
    }

    public void stopSleepMonitoring() {
        if (!isMonitoring) {
            return;
        }

        isMonitoring = false;

        if (currentSession != null) {
            finalizeSleepSession();
        }

        Log.d(TAG, "Sleep monitoring stopped");
    }

    // Main method to feed heart rate data
    public void updateHeartRate(int heartRate) {
        if (!isMonitoring) {
            Log.w(TAG, "Not monitoring, heart rate update ignored");
            return;
        }

        this.currentHeartRate = heartRate;
        long currentTime = System.currentTimeMillis();

        Log.d(TAG, "Heart Rate updated: " + heartRate + " BPM");
        analyzeSleepState(currentTime);
    }

    // Method to feed movement data (if available from accelerometer)
    public void updateMovementLevel(double movementLevel) {
        if (!isMonitoring) {
            return;
        }

        this.currentMovementLevel = movementLevel;
        long currentTime = System.currentTimeMillis();

        // Update movement time if significant movement detected
        if (movementLevel > MOVEMENT_THRESHOLD) {
            lastMovementTime = currentTime;
            Log.d(TAG, "Movement detected: " + String.format("%.2f", movementLevel));
        }
    }

    // Alternative method to feed accelerometer data directly
    public void updateAccelerometerData(float[] acceleration) {
        if (!isMonitoring || acceleration.length < 3) {
            return;
        }

        double movementLevel = calculateMovementMagnitude(acceleration);
        updateMovementLevel(movementLevel);
    }

    private double calculateMovementMagnitude(float[] acceleration) {
        // Calculate the magnitude of acceleration vector
        // Remove gravity component (approximately 9.8 m/sÂ²)
        double magnitude = Math.sqrt(
                acceleration[0] * acceleration[0] +
                        acceleration[1] * acceleration[1] +
                        acceleration[2] * acceleration[2]
        );

        // Subtract gravity to get movement-only acceleration
        return Math.abs(magnitude - 9.8);
    }

    private void analyzeSleepState(long currentTime) {
        // Determine sleep state based on HR and movement
        SleepState newSleepState = determineSleepState(currentHeartRate, currentMovementLevel, currentTime);

        // Check if sleep state changed
        if (newSleepState != currentSleepState) {
            Log.d(TAG, "Sleep state changed: " + currentSleepState + " -> " + newSleepState);
            updateSleepState(newSleepState, currentTime);
        }

        // Record data point
        SleepDataPoint dataPoint = new SleepDataPoint(
                currentTime, currentHeartRate, currentMovementLevel, currentSleepState);
        sleepData.add(dataPoint);

        // Limit data storage (keep last 8 hours worth)
        if (sleepData.size() > 480) { // 480 samples at 1 minute intervals = 8 hours
            sleepData.remove(0);
        }
    }

    private SleepState determineSleepState(int heartRate, double movementLevel, long currentTime) {
        // Time since last significant movement
        long timeSinceMovement = currentTime - lastMovementTime;

        // Calculate heart rate relative to baseline
        double hrRatio = (double) heartRate / baselineHeartRate;

        // Sleep detection logic
        if (movementLevel > MOVEMENT_THRESHOLD ||
                timeSinceMovement < SLEEP_DETECTION_WINDOW) {
            return SleepState.AWAKE;
        }

        // If no movement for sleep detection window
        if (timeSinceMovement >= SLEEP_DETECTION_WINDOW) {

            // Deep sleep: very low HR and no movement for extended period
            if (hrRatio < 0.75 && timeSinceMovement >= DEEP_SLEEP_WINDOW) {
                return SleepState.DEEP_SLEEP;
            }

            // REM sleep: HR closer to baseline but still no movement
            else if (hrRatio >= 0.85 && hrRatio <= 1.1 && timeSinceMovement >= DEEP_SLEEP_WINDOW) {
                return SleepState.REM_SLEEP;
            }

            // Light sleep: reduced HR, no recent movement
            else if (hrRatio < SLEEP_HR_THRESHOLD_FACTOR) {
                return SleepState.LIGHT_SLEEP;
            }
        }

        return SleepState.AWAKE;
    }

    private void updateSleepState(SleepState newState, long currentTime) {
        // Update duration for previous state
        if (currentSleepState != SleepState.AWAKE && currentSession != null) {
            long stateDuration = currentTime - sleepStateStartTime;

            switch (currentSleepState) {
                case DEEP_SLEEP:
                    currentSession.deepSleepDuration += stateDuration;
                    break;
                case LIGHT_SLEEP:
                    currentSession.lightSleepDuration += stateDuration;
                    break;
                case REM_SLEEP:
                    currentSession.remSleepDuration += stateDuration;
                    break;
                case AWAKE:
                    if (sleepStateStartTime > 0) {
                        currentSession.awakePeriods++;
                    }
                    break;
            }
        }

        // Set new state
        currentSleepState = newState;
        sleepStateStartTime = currentTime;

        // If transitioning from awake to sleep, mark sleep start
        if (currentSleepState != SleepState.AWAKE && currentSession.sleepStartTime == 0) {
            currentSession.sleepStartTime = currentTime;
            Log.d(TAG, "Sleep session started");
        }
    }

    private void finalizeSleepSession() {
        if (currentSession == null) return;

        long currentTime = System.currentTimeMillis();
        currentSession.sleepEndTime = currentTime;
        currentSession.totalSleepDuration =
                currentSession.deepSleepDuration +
                        currentSession.lightSleepDuration +
                        currentSession.remSleepDuration;

        // Calculate average heart rate during sleep
        if (!sleepData.isEmpty()) {
            int totalHR = 0;
            int sleepReadings = 0;

            for (SleepDataPoint point : sleepData) {
                if (point.sleepState != SleepState.AWAKE) {
                    totalHR += point.heartRate;
                    sleepReadings++;
                }
            }

            currentSession.averageHeartRate = sleepReadings > 0 ? totalHR / sleepReadings : 0;
        }

        // Calculate sleep efficiency
        long totalTimeInBed = currentSession.sleepEndTime - currentSession.sleepStartTime;
        if (totalTimeInBed > 0) {
            currentSession.sleepEfficiency = (int) ((double) currentSession.totalSleepDuration / totalTimeInBed * 100);
        }

        Log.d(TAG, "Sleep session completed: " + currentSession.toString());
    }

    // Public methods for configuration and data access
    public void setBaselineHeartRate(double baselineHR) {
        this.baselineHeartRate = baselineHR;
        Log.d(TAG, "Baseline heart rate set to: " + baselineHR);
    }

    public double getBaselineHeartRate() {
        return baselineHeartRate;
    }

    public SleepState getCurrentSleepState() {
        return currentSleepState;
    }

    public SleepSession getCurrentSession() {
        return currentSession;
    }

    public List<SleepDataPoint> getSleepData() {
        return new ArrayList<>(sleepData);
    }

    // Get sleep statistics
    public SleepStatistics getSleepStatistics() {
        if (currentSession == null) {
            return null;
        }

        return new SleepStatistics(currentSession, sleepData);
    }

    // Sleep statistics helper class
    public static class SleepStatistics {
        public final long totalSleepTime;
        public final long deepSleepTime;
        public final long lightSleepTime;
        public final long remSleepTime;
        public final int averageHeartRate;
        public final int lowestHeartRate;
        public final int highestHeartRate;
        public final int sleepEfficiency;
        public final int awakePeriods;
        public final long fallAsleepTime; // Time to fall asleep

        public SleepStatistics(SleepSession session, List<SleepDataPoint> data) {
            this.totalSleepTime = session.totalSleepDuration;
            this.deepSleepTime = session.deepSleepDuration;
            this.lightSleepTime = session.lightSleepDuration;
            this.remSleepTime = session.remSleepDuration;
            this.averageHeartRate = session.averageHeartRate;
            this.sleepEfficiency = session.sleepEfficiency;
            this.awakePeriods = session.awakePeriods;

            // Calculate min/max HR during sleep
            int minHR = Integer.MAX_VALUE;
            int maxHR = Integer.MIN_VALUE;
            long firstSleepTime = 0;

            for (SleepDataPoint point : data) {
                if (point.sleepState != SleepState.AWAKE) {
                    if (firstSleepTime == 0) {
                        firstSleepTime = point.timestamp;
                    }
                    minHR = Math.min(minHR, point.heartRate);
                    maxHR = Math.max(maxHR, point.heartRate);
                }
            }

            this.lowestHeartRate = minHR == Integer.MAX_VALUE ? 0 : minHR;
            this.highestHeartRate = maxHR == Integer.MIN_VALUE ? 0 : maxHR;
            this.fallAsleepTime = firstSleepTime > 0 ? firstSleepTime - session.sleepStartTime : 0;
        }

        @NonNull
        @SuppressLint("DefaultLocale")
        @Override
        public String toString() {
            return String.format("Sleep Statistics:\n" +
                            "Total Sleep: %d hrs %d min\n" +
                            "Deep Sleep: %d min (%.1f%%)\n" +
                            "Light Sleep: %d min (%.1f%%)\n" +
                            "REM Sleep: %d min (%.1f%%)\n" +
                            "Heart Rate: %d avg (range: %d-%d)\n" +
                            "Sleep Efficiency: %d%%\n" +
                            "Awake Periods: %d\n" +
                            "Time to Fall Asleep: %d min",
                    totalSleepTime / (1000 * 60 * 60),
                    (totalSleepTime % (1000 * 60 * 60)) / (1000 * 60),
                    deepSleepTime / (1000 * 60),
                    totalSleepTime > 0 ? (double) deepSleepTime / totalSleepTime * 100 : 0,
                    lightSleepTime / (1000 * 60),
                    totalSleepTime > 0 ? (double) lightSleepTime / totalSleepTime * 100 : 0,
                    remSleepTime / (1000 * 60),
                    totalSleepTime > 0 ? (double) remSleepTime / totalSleepTime * 100 : 0,
                    averageHeartRate, lowestHeartRate, highestHeartRate,
                    sleepEfficiency, awakePeriods,
                    fallAsleepTime / (1000 * 60));
        }
    }

    // Smart alarm feature - wake during light sleep
    public boolean isGoodTimeToWake(long targetWakeTime, long wakeWindowMinutes) {
        long currentTime = System.currentTimeMillis();
        long wakeWindow = wakeWindowMinutes * 60 * 1000; // Convert to milliseconds

        // Check if we're within the wake window
        if (currentTime >= (targetWakeTime - wakeWindow) && currentTime <= targetWakeTime) {
            // Wake if in light sleep or REM sleep
            return currentSleepState == SleepState.LIGHT_SLEEP ||
                    currentSleepState == SleepState.REM_SLEEP ||
                    currentSleepState == SleepState.AWAKE;
        }

        return false;
    }

    // Sleep quality score (0-100)
    public int calculateSleepQuality() {
        if (currentSession == null || currentSession.totalSleepDuration == 0) {
            return 0;
        }

        int score = 100;

        // Deduct points for poor sleep efficiency
        if (currentSession.sleepEfficiency < 85) {
            score -= (85 - currentSession.sleepEfficiency);
        }

        // Deduct points for too many awake periods
        if (currentSession.awakePeriods > 3) {
            score -= (currentSession.awakePeriods - 3) * 5;
        }

        // Deduct points for insufficient deep sleep (should be ~20% of total)
        if (currentSession.totalSleepDuration > 0) {
            double deepSleepPercentage = (double) currentSession.deepSleepDuration / currentSession.totalSleepDuration * 100;
            if (deepSleepPercentage < 15) {
                score -= (int) (15 - deepSleepPercentage) * 2;
            }
        }

        // Ensure score stays within 0-100 range
        return Math.max(0, Math.min(100, score));
    }

    // Getters and utility methods
    public boolean isCurrentlyMonitoring() {
        return isMonitoring;
    }

    public long getCurrentSessionDuration() {
        if (currentSession == null) {
            return 0;
        }
        return System.currentTimeMillis() - currentSession.sleepStartTime;
    }

    public void calibrateBaselineHeartRate() {
        // Calculate baseline from recent awake periods
        List<Integer> awakeHeartRates = new ArrayList<>();
        long recentTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // Last 24 hours

        for (SleepDataPoint point : sleepData) {
            if (point.timestamp > recentTime && point.sleepState == SleepState.AWAKE) {
                awakeHeartRates.add(point.heartRate);
            }
        }

        if (!awakeHeartRates.isEmpty()) {
            double sum = awakeHeartRates.stream().mapToInt(Integer::intValue).sum();
            baselineHeartRate = sum / awakeHeartRates.size();
            Log.d(TAG, "Calibrated baseline heart rate: " + baselineHeartRate);
        }
    }
}