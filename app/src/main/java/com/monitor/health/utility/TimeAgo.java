package com.monitor.health.utility;


public final class TimeAgo {
    private TimeAgo() {}

    /** Convert ISO-8601 UTC (e.g. 2025-09-17T06:49:43.136000Z) to "x min ago"/"x days ago". */
    public static String relativeFromIsoUtc(String isoUtc) {
        if (isoUtc == null || isoUtc.isEmpty()) return "-";
        long thenMillis = parseIsoToMillis(isoUtc);
        if (thenMillis <= 0L) return "-";

        long nowMillis = System.currentTimeMillis();
        long diff = nowMillis - thenMillis;

        // Future timestamps
        if (diff < 0) {
            long ahead = -diff;
            long mins = ahead / java.util.concurrent.TimeUnit.MINUTES.toMillis(1);
            if (mins < 1) return "in a few seconds";
            if (mins < 60) return "in " + mins + " min";
            long hours = ahead / java.util.concurrent.TimeUnit.HOURS.toMillis(1);
            if (hours < 24) return "in " + hours + (hours == 1 ? " hour" : " hours");
            long days = ahead / java.util.concurrent.TimeUnit.DAYS.toMillis(1);
            return "in " + days + (days == 1 ? " day" : " days");
        }

        // Past timestamps
        long mins = diff / java.util.concurrent.TimeUnit.MINUTES.toMillis(1);
        if (mins < 1) return "just now";
        if (mins < 60) return mins + " min ago";

        long hours = diff / java.util.concurrent.TimeUnit.HOURS.toMillis(1);
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");

        long days = diff / java.util.concurrent.TimeUnit.DAYS.toMillis(1);
        // Always show days (so 2025-09-13 vs 2025-09-22 becomes "9 days ago")
        return days + (days == 1 ? " day ago" : " days ago");
    }

    /** Robust ISO parser: uses java.time when available, otherwise falls back to SimpleDateFormat. */
    private static long parseIsoToMillis(String isoUtc) {
        try {
            return java.time.Instant.parse(isoUtc).toEpochMilli();
        } catch (Throwable ignore) {
            // Fall through to legacy parser
        }
        String s = isoUtc;
        s = s.replaceFirst("(\\.\\d{3})\\d+Z$", "$1Z");
        if (!s.matches(".*\\.\\d{3}Z$")) {
            s = s.replace("Z", ".000Z");
        }
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        try {
            return sdf.parse(s).getTime();
        } catch (java.text.ParseException e) {
            return 0L;
        }
    }
}
