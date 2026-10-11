package io.github.luigichopper.palimpsest.puzzle;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class AnswerVerifier {

    public static String normalize(String rawInput, List<String> rules) {
        if (rawInput == null) {
            return "";
        }
        String result = rawInput;
        if (rules == null || rules.isEmpty()) {
            return result.trim();
        }

        for (String rule : rules) {
            if (rule == null) continue;
            switch (rule.toLowerCase().trim()) {
                case "trim" -> result = result.trim();
                case "lowercase" -> result = result.toLowerCase();
                case "uppercase" -> result = result.toUpperCase();
                case "collapse_ws" -> result = result.replaceAll("\\s+", " ").trim();
                case "strip_punct" -> result = result.replaceAll("[^a-zA-Z0-9\\s]", "");
                case "digits_only" -> result = result.replaceAll("[^0-9]", "");
                case "octal_to_decimal" -> {
                    String digits = result.replaceAll("[^0-7]", "");
                    if (!digits.isEmpty()) {
                        try {
                            long dec = Long.parseLong(digits, 8);
                            result = Long.toString(dec);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                default -> {
                    // Unknown rule; ignore
                }
            }
        }
        return result;
    }

    public static String computeHash(String algo, String salt, String normalized) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algo != null ? algo : "SHA-256");
            String payload = (salt != null ? salt : "") + (normalized != null ? normalized : "");
            byte[] hashBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unsupported hash algorithm: " + algo, e);
        }
    }

    public static boolean verify(String rawInput, List<String> rules, String algo, String salt, String expectedHash) {
        if (expectedHash == null || expectedHash.isEmpty()) {
            return false;
        }
        String norm = normalize(rawInput, rules);
        String actualHash = computeHash(algo, salt, norm);
        return expectedHash.equalsIgnoreCase(actualHash);
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
