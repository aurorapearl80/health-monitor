package com.monitor.health.utility;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    /**
     * Convert milliseconds timestamp to relative time string (e.g., "5 min ago", "2 hours ago")
     */
    public static String getRelativeTime(long timestampMillis) {
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - timestampMillis;

        // Convert to seconds
        long seconds = timeDifference / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + (minutes == 1 ? " min ago" : " mins ago");
        } else if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (days < 7) {
            return days + (days == 1 ? " day ago" : " days ago");
        } else {
            // Format as date if older than a week
            return formatDate(timestampMillis);
        }
    }

    /**
     * Format timestamp to readable date format (e.g., "Mar 8, 2:30 PM")
     */
    public static String formatDate(long timestampMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
        return sdf.format(new Date(timestampMillis));
    }

    /**
     * Format timestamp to full date-time format (e.g., "Mar 8, 2026 2:30:05 PM")
     */
    public static String formatFullDateTime(long timestampMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm:ss a", Locale.getDefault());
        return sdf.format(new Date(timestampMillis));
    }
}