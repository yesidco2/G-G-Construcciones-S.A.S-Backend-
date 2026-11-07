package com.gyg.backend.service;

import com.gyg.backend.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final int CLEANUP_THRESHOLD = 5000;

    private final ConcurrentHashMap<LimitKey, Window> windows = new ConcurrentHashMap<>();
    private final boolean trustProxyHeaders;

    public RateLimitService(
            @Value("${app.security.trust-proxy-headers:false}") boolean trustProxyHeaders) {
        this.trustProxyHeaders = trustProxyHeaders;
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
        if (maxRequests < 1 || windowDuration.isZero() || windowDuration.isNegative()) {
            throw new IllegalArgumentException("El limite y la duracion de la ventana deben ser positivos");
        }

        long now = System.nanoTime();
        long durationNanos = windowDuration.toNanos();
        LimitKey key = new LimitKey(scope, clientIp(request));
        Window updated = windows.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAtNanos() >= current.durationNanos()) {
                return new Window(now, durationNanos, 1);
            }
            return new Window(current.startedAtNanos(), current.durationNanos(), current.requestCount() + 1);
        });

        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.entrySet().removeIf(entry ->
                    now - entry.getValue().startedAtNanos() >= entry.getValue().durationNanos());
        }
        if (updated.requestCount() > maxRequests) {
            throw new TooManyRequestsException(message);
        }
    }

    private String clientIp(HttpServletRequest request) {
        if (trustProxyHeaders) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                String firstAddress = forwardedFor.split(",", 2)[0].trim();
                if (!firstAddress.isEmpty()) {
                    return firstAddress;
                }
            }
        }
        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }

    private record LimitKey(String scope, String clientIp) {
    }

    private record Window(long startedAtNanos, long durationNanos, int requestCount) {
    }
}
