package com.gyg.backend.service;

import com.gyg.backend.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Objects;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RateLimitService {

    private static final long CLEANUP_EVERY_REQUESTS = 512;
    private static final long RETAIN_EXPIRED_MILLIS = Duration.ofDays(1).toMillis();

    private final PersistentRateLimitStore store;
    private final AtomicLong requestsSinceCleanup = new AtomicLong();

    public RateLimitService(PersistentRateLimitStore store) {
        this.store = store;
    }

    public void check(
            String scope,
            HttpServletRequest request,
            int maxRequests,
            Duration windowDuration,
            String message) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(windowDuration, "windowDuration");
        if (scope.isBlank() || scope.length() > 200
                || maxRequests < 1 || windowDuration.isZero() || windowDuration.isNegative()) {
            throw new IllegalArgumentException("El alcance, el límite y la duración deben ser válidos.");
        }

        long durationMillis = Math.max(1, windowDuration.toMillis());
        String clientHash = hash(clientIp(request));
        PersistentRateLimitStore.Window updated =
                store.increment(scope, clientHash, durationMillis);

        if (updated.requestCount() > maxRequests) {
            long remainingMillis = Math.max(0, updated.expiresAt() - updated.nowMillis());
            long retryAfterSeconds = Math.max(
                    1, remainingMillis / 1000 + (remainingMillis % 1000 == 0 ? 0 : 1));
            throw new TooManyRequestsException(message, retryAfterSeconds);
        }

        if (requestsSinceCleanup.incrementAndGet() % CLEANUP_EVERY_REQUESTS == 0) {
            store.deleteExpiredBefore(RETAIN_EXPIRED_MILLIS);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No se pudo calcular la huella de la dirección cliente.", exception);
        }
    }
}
