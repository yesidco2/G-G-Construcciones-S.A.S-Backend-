package com.gyg.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

@Service
public class TrackingCodeService {

    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String pepper;

    public TrackingCodeService(@Value("${app.tracking-code.pepper:}") String pepper) {
        if (pepper == null || pepper.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("APP_TRACKING_CODE_PEPPER debe tener al menos 32 bytes.");
        }
        this.pepper = pepper;
    }

    public GeneratedTrackingCode generate() {
        StringBuilder value = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            value.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        String code = value.toString();
        return new GeneratedTrackingCode(code, hash(code));
    }

    public String hash(String code) {
        return hmacSha256(code, pepper);
    }

    public boolean matches(String submittedCode, String storedHash) {
        byte[] expected = HexFormat.of().parseHex(hash(submittedCode == null ? "" : submittedCode));
        byte[] actual = new byte[32];
        boolean validStoredHash = storedHash != null;
        try {
            byte[] parsed = storedHash == null ? new byte[0] : HexFormat.of().parseHex(storedHash);
            if (parsed.length == actual.length) {
                actual = parsed;
            } else {
                validStoredHash = false;
            }
        } catch (IllegalArgumentException exception) {
            validStoredHash = false;
        }
        boolean equal = MessageDigest.isEqual(expected, actual);
        return validStoredHash && equal;
    }

    public static String hmacSha256(String value, String pepper) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No fue posible procesar el código de seguimiento.", exception);
        }
    }

    public record GeneratedTrackingCode(String value, String hash) {
    }
}
