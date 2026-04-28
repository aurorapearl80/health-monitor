package com.monitor.health.utility;

import android.util.Log;

public class BloodPressureEstimator {

    private static final String TAG = "BloodPressureEstimator";

    // Default baseline values - can be adjusted based on population averages
    private static final double DEFAULT_BASELINE_HR = 70.0;
    private static final double DEFAULT_BASELINE_SYSTOLIC = 120.0;
    private static final double DEFAULT_BASELINE_DIASTOLIC = 80.0;

    // Instance variables for user-specific baselines (can be set via setters)
    private double userBaselineHR = DEFAULT_BASELINE_HR;
    private double userBaselineSystolic = DEFAULT_BASELINE_SYSTOLIC;
    private double userBaselineDiastolic = DEFAULT_BASELINE_DIASTOLIC;

    // Default constructor uses population averages
    public BloodPressureEstimator() {
        // Uses default values
    }

    // Constructor with custom baseline values
    public BloodPressureEstimator(double baselineHR, double baselineSystolic, double baselineDiastolic) {
        this.userBaselineHR = baselineHR;
        this.userBaselineSystolic = baselineSystolic;
        this.userBaselineDiastolic = baselineDiastolic;
    }

    public BPReading estimateBloodPressure(int currentHeartRate) {
        // Very simplified estimation - NOT medically accurate
        // This is just for demonstration

        double hrDelta = currentHeartRate - userBaselineHR;

        // Rough correlation: higher HR often correlates with higher BP
        double systolicAdjustment = hrDelta * 0.5; // Adjust factor based on research
        double diastolicAdjustment = hrDelta * 0.3;

        int estimatedSystolic = (int) (userBaselineSystolic + systolicAdjustment);
        int estimatedDiastolic = (int) (userBaselineDiastolic + diastolicAdjustment);

        // Clamp to reasonable ranges
        estimatedSystolic = Math.max(80, Math.min(200, estimatedSystolic));
        estimatedDiastolic = Math.max(50, Math.min(120, estimatedDiastolic));

        return new BPReading(estimatedSystolic, estimatedDiastolic);
    }

    // Setter methods to customize baseline values if needed
    public void setUserBaselines(double baselineHR, double baselineSystolic, double baselineDiastolic) {
        this.userBaselineHR = baselineHR;
        this.userBaselineSystolic = baselineSystolic;
        this.userBaselineDiastolic = baselineDiastolic;
    }

    public void setBaselineHeartRate(double baselineHR) {
        this.userBaselineHR = baselineHR;
    }

    public void setBaselineBloodPressure(double systolic, double diastolic) {
        this.userBaselineSystolic = systolic;
        this.userBaselineDiastolic = diastolic;
    }

    // Getter methods for current baseline values
    public double getBaselineHeartRate() {
        return userBaselineHR;
    }

    public double getBaselineSystolic() {
        return userBaselineSystolic;
    }

    public double getBaselineDiastolic() {
        return userBaselineDiastolic;
    }

    // Example usage method (you can remove this if not needed)
    public void demonstrateEstimation(int heartRate) {
        BPReading estimate = estimateBloodPressure(heartRate);
        Log.d(TAG, "Estimated BP for HR " + heartRate + ": " + estimate.toString());
    }
}