package com.aiops.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public final class JwtUtil {

    private JwtUtil() {
    }

    public static String createToken(Map<String, Object> claims, String secret, long expireSeconds) {
        long exp = Instant.now().getEpochSecond() + expireSeconds;
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url(toJson(claims, exp));
        String signature = hmacSha256(header + "." + payload, secret);
        return header + "." + payload + "." + signature;
    }

    public static Map<String, Object> parseToken(String token, String secret) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT token");
        }
        String expectedSignature = hmacSha256(parts[0] + "." + parts[1], secret);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new IllegalArgumentException("Invalid JWT signature");
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Map<String, Object> claims = parseFlatJson(payload);
        Object exp = claims.get("exp");
        if (exp instanceof Number && ((Number) exp).longValue() < Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("JWT token expired");
        }
        return claims;
    }

    private static String toJson(Map<String, Object> claims, long exp) {
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        claims.forEach((key, value) -> joiner.add("\"" + escape(key) + "\":" + jsonValue(value)));
        joiner.add("\"exp\":" + exp);
        return joiner.toString();
    }

    private static String jsonValue(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create JWT signature", exception);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length(); i++) {
            result |= left.charAt(i) ^ right.charAt(i);
        }
        return result == 0;
    }

    private static Map<String, Object> parseFlatJson(String json) {
        Map<String, Object> result = new HashMap<>();
        String content = json.trim();
        if (content.startsWith("{")) {
            content = content.substring(1);
        }
        if (content.endsWith("}")) {
            content = content.substring(0, content.length() - 1);
        }
        if (content.isBlank()) {
            return result;
        }
        for (String pair : content.split(",")) {
            String[] parts = pair.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            String key = unquote(parts[0].trim());
            String rawValue = parts[1].trim();
            if (rawValue.startsWith("\"") && rawValue.endsWith("\"")) {
                result.put(key, unquote(rawValue));
            } else {
                try {
                    result.put(key, Long.parseLong(rawValue));
                } catch (NumberFormatException exception) {
                    result.put(key, rawValue);
                }
            }
        }
        return result;
    }

    private static String unquote(String value) {
        String result = value;
        if (result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length() - 1);
        }
        return result.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
