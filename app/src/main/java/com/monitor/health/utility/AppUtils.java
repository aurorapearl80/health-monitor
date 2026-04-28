package com.monitor.health.utility;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.monitor.health.Constant;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class AppUtils {

    public static int getVersionCode(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
            return info.versionCode; // Deprecated in API 28
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static String getTodayDate() {
        // Get the current date and time
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

        // Set the desired time zone offset (e.g., +08:00)
        TimeZone timeZone = TimeZone.getTimeZone("GMT+08:00");
        calendar.setTimeZone(timeZone);

        // Format the date and time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(timeZone);
        String formattedDate = sdf.format(currentDate);

        // Get the offset in hours and minutes
        int offsetMillis = timeZone.getOffset(currentDate.getTime());
        int offsetHours = offsetMillis / (60 * 60 * 1000);
        int offsetMinutes = Math.abs((offsetMillis / (60 * 1000)) % 60);

        // Format the offset
        String offset = String.format("%s%02d:%02d", offsetHours >= 0 ? "+" : "-", Math.abs(offsetHours), offsetMinutes);


        // Combine formatted date and offset
        String finalFormattedDate = formattedDate +""+offset;

        return finalFormattedDate;

    }

    /**
     * Calculate BMI using weight (kg) and height (feet) from PreferenceHelper.
     * @param context  Android Context (needed to access PreferenceHelper)
     * @param weightKg Weight in kilograms
     * @return BMI rounded to 1 decimal place
     */

    public static double calculateBMI(Context context, double weightKg) {
        String heightStr = PreferenceHelper.getInstance(context)
                .getString(Constant.userHeight, "");

        double heightMeters = parseHeightToMeters(heightStr);
        if (heightMeters <= 0) return 0.0;

        double bmi = weightKg / (heightMeters * heightMeters);
        return Math.round(bmi * 10.0) / 10.0; // 1 decimal
    }

    private static double parseHeightToMeters(String raw) {
        if (raw == null) return 0.0;

        String s = raw.trim();
        if (s.isEmpty()) return 0.0;

        // normalize smart quotes
        s = s.replace("â€™", "'")
                .replace("â€³", "\"")
                .replace("â€œ", "\"")
                .replace("â€", "\"");

        // Case 1: feet'inches" e.g. 6'1" or 6' 1"
        if (s.contains("'")) {
            try {
                String[] parts = s.split("'");
                int feet = Integer.parseInt(parts[0].trim());

                int inches = 0;
                if (parts.length > 1) {
                    String inchPart = parts[1].replace("\"", "").trim();
                    if (!inchPart.isEmpty()) inches = Integer.parseInt(inchPart);
                }

                double totalInches = feet * 12.0 + inches;
                return totalInches * 0.0254; // inches -> meters
            } catch (Exception ignored) {
                return 0.0;
            }
        }

        // Remove unit labels if present
        String lower = s.toLowerCase();
        try {
            if (lower.endsWith("cm")) {
                double cm = Double.parseDouble(lower.replace("cm", "").trim().replace(",", "."));
                return cm / 100.0;
            }

            if (lower.endsWith("m")) {
                double m = Double.parseDouble(lower.substring(0, lower.length() - 1).trim().replace(",", "."));
                return m;
            }

            // Plain number:
            // If your app previously stored feet as number (e.g., "5.9"), treat as feet
            double val = Double.parseDouble(s.replace(",", "."));

            // Heuristic:
            // If val looks like cm (e.g. 150-220), treat as cm; otherwise treat as feet
            if (val >= 100 && val <= 250) return val / 100.0;  // cm -> m
            return val * 0.3048; // feet -> m
        } catch (Exception ignored) {
            return 0.0;
        }
    }

//    public static double calculateBMI(Context context, double weightKg) {
//        double heightFeet = 0.0;
//
//        try {
//            String heightStr = PreferenceHelper.getInstance(context)
//                    .getString(Constant.userHeight, "0");
//            heightFeet = Double.parseDouble(heightStr);
//        } catch (NumberFormatException e) {
//            e.printStackTrace();
//        }
//
//        if (heightFeet <= 0) return 0.0;
//
//        // Convert feet to meters
//        double heightMeters = heightFeet * 0.3048;
//
//        // BMI formula
//        double bmi = weightKg / (heightMeters * heightMeters);
//
//        // Round to 1 decimal place
//        return Math.round(bmi * 10.0) / 10.0;
//    }

    /**
     * Get a description of BMI category.
     * @param bmi Calculated BMI value
     * @return Description string
     */
    public static String getBmiDescription(double bmi) {
        if (bmi < 2) return "Good"; // default for no BMI value
        else if (bmi < 18.5) return "Underweight";
        else if (bmi <= 24.9) return "Normal"; // healthy
        else if (bmi <= 29.9) return "Overweight";
        else if (bmi <= 34.9) return "Obese I"; // obese class 1
        else if (bmi <= 39.9) return "Obese II"; // obese class 2
        else return "Obese III"; // extremely obese
    }
}
