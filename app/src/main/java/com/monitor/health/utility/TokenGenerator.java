package com.monitor.health.utility;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TokenGenerator {

    public static String token() {
        try {
            // Convert hex string to byte array (hex2bin equivalent)
            String hexString = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            byte[] secret = hexStringToByteArray(hexString);

            // Get current date in 'ddMM' format (Day-Month)
            String date = new SimpleDateFormat("ddMM").format(new Date());

            // Generate HMAC-SHA256 token
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret, "HmacSHA256");
            mac.init(secretKey);

            byte[] hmacBytes = mac.doFinal(date.getBytes(StandardCharsets.UTF_8));
            String token = bytesToHex(hmacBytes);  // Convert HMAC bytes to hex string


            return token;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    // Helper method to convert hex string to byte array
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    // Helper method to convert byte array to hex string
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

