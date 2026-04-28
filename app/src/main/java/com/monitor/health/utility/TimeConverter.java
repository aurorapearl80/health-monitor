package com.monitor.health.utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeConverter {

    // Function to convert time to military format and return the hour
    public static String getMilitaryHour(String timeIn12HourFormat) {
        try {
            // Define the input formatter for the 12-hour format
            SimpleDateFormat inputFormat = new SimpleDateFormat("hh:mm a", Locale.US);

            // Define the output formatter for the 24-hour format
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH", Locale.US);

            // Parse the input string to a Date object
            Date date = inputFormat.parse(timeIn12HourFormat);

            // Format and return the hour in 24-hour format
            return outputFormat.format(date);

        } catch (ParseException e) {
            // Handle the case where the input string is not valid
            System.err.println("Invalid time format: " + e.getMessage());
            return null;
        }
    }

    // Function to convert time to military format and return the minutes
    public static String getMilitaryMinutes(String timeIn12HourFormat) {
        try {
            // Define the input formatter for the 12-hour format
            SimpleDateFormat inputFormat = new SimpleDateFormat("hh:mm a", Locale.US);

            // Define the output formatter for the minutes
            SimpleDateFormat outputFormat = new SimpleDateFormat("mm", Locale.US);

            // Parse the input string to a Date object
            Date date = inputFormat.parse(timeIn12HourFormat);

            // Format and return the minutes
            return outputFormat.format(date);

        } catch (ParseException e) {
            // Handle the case where the input string is not valid
            System.err.println("Invalid time format: " + e.getMessage());
            return null;
        }
    }

    // Function to convert an integer to its hexadecimal value
    public static String convertToHexadecimal(int number) {
        // Convert the integer to its hexadecimal representation
        return Integer.toHexString(number);
    }
}
