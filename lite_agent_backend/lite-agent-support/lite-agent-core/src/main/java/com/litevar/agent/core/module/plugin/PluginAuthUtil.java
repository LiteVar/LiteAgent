package com.litevar.agent.core.module.plugin;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Plugin HMAC signer.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
public class PluginAuthUtil {
    private PluginAuthUtil() {
    }

    public static SignedHeaders sign(byte[] sharedKey, String method, String path, String query, byte[] body,
                                     String contentType) {
        String ts = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String bodySha = body == null || body.length == 0 ? "" : sha256Hex(body);
        String canonical = method.toUpperCase() + "\n"
                + path + "\n"
                + ts + "\n"
                + nonce + "\n"
                + (query == null ? "" : query) + "\n"
                + bodySha + "\n"
                + (contentType == null ? "" : contentType);
        String sign = hmacSha256(sharedKey, canonical);
        return new SignedHeaders(ts, nonce, sign);
    }

    public static String normalizeQuery(Map<String, List<String>> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        List<String> pairs = new ArrayList<>();
        params.keySet().stream().sorted().forEach(key -> {
            List<String> values = params.get(key);
            if (values == null || values.isEmpty()) {
                pairs.add(encode(key) + "=");
                return;
            }
            List<String> sorted = new ArrayList<>(values);
            sorted.sort(String::compareTo);
            for (String value : sorted) {
                pairs.add(encode(key) + "=" + encode(value));
            }
        });
        return String.join("&", pairs);
    }

    private static String hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String encode(String value) {
        if (value == null) {
            return "";
        }
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
        return encoded.replace("+", "%20");
    }

    public record SignedHeaders(String ts, String nonce, String sign) {
    }
}
