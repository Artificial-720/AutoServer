package me.artificial.autoserver.common;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class HMAC {
    public static String signMessage(String message, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(message.getBytes());
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    public static boolean verifyMessage(String message, String receivedSignature, String secret) throws Exception {
        String expectedSignature = signMessage(message, secret);
        return expectedSignature.equals(receivedSignature);
    }
}
