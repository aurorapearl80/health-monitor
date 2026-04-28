package com.monitor.health.utility;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateUtils {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("MMM dd", Locale.getDefault()); // e.g., "Sep 29"
    private static final SimpleDateFormat DISPLAY_TIME_FORMAT = new SimpleDateFormat("hh:mm a", Locale.getDefault()); // e.g., "11:00 PM"

    /**
     * Converts a timestamp (milliseconds) to human readable format like "10 days ago"
     * @param timestamp The timestamp in milliseconds
     * @return Human readable time string
     */
    public static String getTimeAgo(long timestamp) {
        if (timestamp <= 0) {
            return "Unknown";
        }

        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - timestamp;

        // If the timestamp is in the future
        if (timeDifference < 0) {
            timeDifference = Math.abs(timeDifference);
            return getTimeInFuture(timeDifference);
        }

        // Convert to different time units
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeDifference);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeDifference);
        long hours = TimeUnit.MILLISECONDS.toHours(timeDifference);
        long days = TimeUnit.MILLISECONDS.toDays(timeDifference);
        long weeks = days / 7;
        long months = days / 30;
        long years = days / 365;

        if (seconds < 60) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        } else if (hours < 24) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        } else if (days < 7) {
            return days == 1 ? "1 day ago" : days + " days ago";
        } else if (weeks < 4) {
            return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
        } else if (months < 12) {
            return months == 1 ? "1 month ago" : months + " months ago";
        } else {
            return years == 1 ? "1 year ago" : years + " years ago";
        }
    }

    /**
     * Handle future timestamps
     */
    private static String getTimeInFuture(long timeDifference) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeDifference);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeDifference);
        long hours = TimeUnit.MILLISECONDS.toHours(timeDifference);
        long days = TimeUnit.MILLISECONDS.toDays(timeDifference);

        if (seconds < 60) {
            return "In a moment";
        } else if (minutes < 60) {
            return "In " + minutes + " minute" + (minutes == 1 ? "" : "s");
        } else if (hours < 24) {
            return "In " + hours + " hour" + (hours == 1 ? "" : "s");
        } else {
            return "In " + days + " day" + (days == 1 ? "" : "s");
        }
    }

    /**
     * Overloaded method that accepts Date object (converts to timestamp)
     * @param date The date to convert
     * @return Human readable time string
     */
    public static String getTimeAgo(Date date) {
        if (date == null) {
            return "Unknown";
        }
        return getTimeAgo(date.getTime());
    }

    /**
     * Short format version (e.g., "10d", "2h", "5m")
     */
    public static String getTimeAgoShort(long timestamp) {
        if (timestamp <= 0) {
            return "?";
        }

        long currentTime = System.currentTimeMillis();
        long timeDifference = Math.abs(currentTime - timestamp);

        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeDifference);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeDifference);
        long hours = TimeUnit.MILLISECONDS.toHours(timeDifference);
        long days = TimeUnit.MILLISECONDS.toDays(timeDifference);
        long weeks = days / 7;
        long months = days / 30;
        long years = days / 365;

        if (seconds < 60) {
            return "now";
        } else if (minutes < 60) {
            return minutes + "m";
        } else if (hours < 24) {
            return hours + "h";
        } else if (days < 7) {
            return days + "d";
        } else if (weeks < 4) {
            return weeks + "w";
        } else if (months < 12) {
            return months + "mo";
        } else {
            return years + "y";
        }
    }

    /**
     * Convert timestamp to readable date format for debugging
     */
    public static String timestampToDate(long timestamp) {
        if (timestamp <= 0) {
            return "Invalid timestamp";
        }
        Date date = new Date(timestamp);
        return date.toString();
    }

    /**
     * Check if timestamp is valid (not too far in past or future)
     */
    public static boolean isValidTimestamp(long timestamp) {
        long currentTime = System.currentTimeMillis();
        long oneYearInMs = 365L * 24 * 60 * 60 * 1000;

        // Check if timestamp is within reasonable range (1 year past to 1 year future)
        return timestamp > (currentTime - oneYearInMs) &&
                timestamp < (currentTime + oneYearInMs);
    }

    /**
     * Get current date as String (yyyy-MM-dd)
     */
    public static String getCurrentDate() {
        Calendar cal = Calendar.getInstance();
        return sdf.format(cal.getTime());
    }

    /**
     * Get previous date by subtracting days
     */
    public static String getPreviousDays(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        return sdf.format(cal.getTime());
    }

    /**
     * Get previous date by subtracting months
     */
    public static String getPreviousMonths(int months) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -months);
        return sdf.format(cal.getTime());
    }

    /**
     * Get previous date by subtracting years
     */
    public static String getPreviousYears(int years) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -years);
        return sdf.format(cal.getTime());
    }

    /**
     * Get today's date
     * @return Today's date in yyyy-MM-dd format
     */
    public static String getToday() {
        return sdf.format(new Date());
    }

    /**
     * Get start date for current week (Monday)
     * @return Start of week date in yyyy-MM-dd format
     */
    public static String getWeekStartDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return sdf.format(calendar.getTime());
    }

    /**
     * Get end date for current week (Sunday)
     * @return End of week date in yyyy-MM-dd format
     */
    public static String getWeekEndDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        return sdf.format(calendar.getTime());
    }

    /**
     * Get start date for current month (1st day of month)
     * @return Start of month date in yyyy-MM-dd format
     */
    public static String getMonthStartDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return sdf.format(calendar.getTime());
    }

    /**
     * Get end date for current month (today)
     * @return Today's date in yyyy-MM-dd format
     */
    public static String getMonthEndDate() {
        return getToday();
    }

    /**
     * Get date from N days ago
     * @param daysAgo Number of days to go back
     * @return Date in yyyy-MM-dd format
     */
    public static String getDateDaysAgo(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -daysAgo);
        return sdf.format(calendar.getTime());
    }

    /**
     * Get date from N months ago
     * @param monthsAgo Number of months to go back
     * @return Date in yyyy-MM-dd format
     */
    public static String getDateMonthsAgo(int monthsAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -monthsAgo);
        return sdf.format(calendar.getTime());
    }

    static {
        ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Format ISO 8601 date string to display format (e.g., "Sep 29")
     * @param isoDateString Date string in format "2025-07-10T18:15:03.000000Z"
     * @return Formatted date string like "Sep 29" or "Jul 10"
     */
    public static String formatDate(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) {
            return "";
        }

        try {
            Date date = ISO_FORMAT.parse(isoDateString);
            if (date != null) {
                return DISPLAY_DATE_FORMAT.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * Format ISO 8601 date string to time format (e.g., "06:15 PM")
     * @param isoDateString Date string in format "2025-07-10T18:15:03.000000Z"
     * @return Formatted time string like "11:00 PM" or "06:15 PM"
     */
    public static String formatTime(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) {
            return "";
        }

        try {
            Date date = ISO_FORMAT.parse(isoDateString);
            if (date != null) {
                return DISPLAY_TIME_FORMAT.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static String getDate() {
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

    public static String toIso8601(Date date, String tzId) {
        java.time.ZoneId zone = java.time.ZoneId.of(tzId); // e.g., "Asia/Manila"
        java.time.ZonedDateTime zdt = java.time.ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(date.getTime()), zone);
        return zdt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
    }

    public static String toIso8601Manila(Date date) {
        return toIso8601(date, "Asia/Manila");
    }

    /** Returns today's date at 00:00:00 UTC, e.g. 2026-04-04T00:00:00Z */
    public static String getTodayIsoStart() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return fmt.format(cal.getTime());
    }

    /** Returns today's date at 23:59:59 UTC, e.g. 2026-04-04T23:59:59Z */
    public static String getTodayIsoEnd() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 0);
        return fmt.format(cal.getTime());
    }

    /** Returns N days ago at 00:00:00 UTC, e.g. 2026-03-28T00:00:00Z */
    public static String getDateDaysAgoIsoStart(int daysAgo) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.add(Calendar.DAY_OF_MONTH, -daysAgo);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return fmt.format(cal.getTime());
    }

}
