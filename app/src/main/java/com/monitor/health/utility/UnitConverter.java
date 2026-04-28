package com.monitor.health.utility;

public class UnitConverter {

    // 1 kilogram = 2.20462 pounds
    // 1 kilogram = 2.20462 pounds
    public static double kiloToPounds(double kilo) {
        double pounds = kilo * 2.20462;
        return Math.round(pounds * 10.0) / 10.0; // round to 1 decimal
    }

    // Convert KG â†’ LBS
    public static double kgToLbs(double kg) {
        return kg * 2.20462;
    }

    public static int kgToLbsWholeNumber(double kg) {
        return (int) Math.round(kg * 2.20462);
    }

    // Convert LBS â†’ KG
    public static double lbsToKg(double lbs) {
        return lbs * 0.453592;
    }

    // (Optional) round to 2 decimal places
    public static String kiloToPoundsString(double kilo) {
        double pounds = kilo * 2.20462;
        return String.format("%.2f", pounds);
    }

    // Convert Celsius to Fahrenheit
    public static double celsiusToFahrenheit(double celsius) {
        return (celsius * 9 / 5) + 32;
    }

    // Convert Fahrenheit to Celsius
    public static double fahrenheitToCelsius(double fahrenheit) {
        return (fahrenheit - 32) * 5 / 9;
    }

    public static double parseHeightToCm(String raw) {
        if (raw == null) return 0.0;

        String s = raw.trim();
        if (s.isEmpty()) return 0.0;

        // normalize quotes
        s = s.replace("â€™", "'").replace("â€³", "\"").replace("â€œ", "\"").replace("â€", "\"");

        // Case 1: feet'inches"  e.g. 6'1" or 6' 1"
        if (s.contains("'")) {
            try {
                String[] parts = s.split("'");
                int feet = Integer.parseInt(parts[0].trim());

                int inches = 0;
                if (parts.length > 1) {
                    String inchPart = parts[1].replace("\"", "").trim();
                    if (!inchPart.isEmpty()) inches = Integer.parseInt(inchPart);
                }

                return (feet * 30.48) + (inches * 2.54); // cm
            } catch (Exception ignored) {
                return 0.0;
            }
        }

        // Case 2: has unit "cm"
        if (s.toLowerCase().contains("cm")) {
            s = s.toLowerCase().replace("cm", "").trim();
        }

        // Case 3: meters like 1.75m or 1.75
        if (s.toLowerCase().endsWith("m")) {
            s = s.substring(0, s.length() - 1).trim();
            try {
                double meters = Double.parseDouble(s);
                return meters * 100.0;
            } catch (Exception ignored) {
                return 0.0;
            }
        }

        // Case 4: plain number â†’ assume cm (or change assumption if your app uses meters)
        try {
            // also handle commas like "175,5"
            s = s.replace(",", ".");
            return Double.parseDouble(s);
        } catch (Exception ignored) {
            return 0.0;
        }
    }

}

