package com.gyg.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Base64;
import java.util.Arrays;

@Service
public class TrackingCodeService {

    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 32;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final String pepper;
    private final SecretKeySpec encryptionKey;

    public TrackingCodeService(@Value("${app.tracking-code.pepper:}") String pepper) {
        if (pepper == null || pepper.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("APP_TRACKING_CODE_PEPPER debe tener al menos 32 bytes.");
        }
        this.pepper = pepper;
        this.encryptionKey = new SecretKeySpec(
                HexFormat.of().parseHex(hmacSha256("gyg/tracking-code/encryption/v1", pepper)), "AES");
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

    public String encrypt(String code, String hash) {
        byte[] nonce = new byte[NONCE_LENGTH];
        RANDOM.nextBytes(nonce);
        try {
            Cipher cipher = trackingCipher(Cipher.ENCRYPT_MODE, nonce, hash);
            byte[] ciphertext = cipher.doFinal(code.getBytes(StandardCharsets.UTF_8));
            byte[] envelope = Arrays.copyOf(nonce, nonce.length + ciphertext.length);
            System.arraycopy(ciphertext, 0, envelope, nonce.length, ciphertext.length);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(envelope);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No fue posible guardar el código de seguimiento.", exception);
        }
    }

    public String decrypt(String encrypted, String hash) {
        if (encrypted == null) {
            return null;
        }
        try {
            byte[] envelope = Base64.getUrlDecoder().decode(encrypted);
            if (envelope.length <= NONCE_LENGTH + TAG_BITS / Byte.SIZE) {
                throw new IllegalArgumentException("Código de seguimiento cifrado incompleto.");
            }
            Cipher cipher = trackingCipher(Cipher.DECRYPT_MODE, Arrays.copyOf(envelope, NONCE_LENGTH), hash);
            return new String(cipher.doFinal(envelope, NONCE_LENGTH, envelope.length - NONCE_LENGTH),
                    StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("No fue posible recuperar el código de seguimiento.", exception);
        }
    }

    private Cipher trackingCipher(int mode, byte[] nonce, String hash) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, encryptionKey, new GCMParameterSpec(TAG_BITS, nonce));
        cipher.updateAAD(hash.getBytes(StandardCharsets.US_ASCII));
        return cipher;
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
