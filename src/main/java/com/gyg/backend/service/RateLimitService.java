package com.gyg.backend.service;

import com.gyg.backend.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    private static final long MAX_CACHE_ENTRIES = 100_000;

    private final Cache<LimitKey, Window> windows = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_ENTRIES)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

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
        Window updated = windows.asMap().compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAtNanos() >= current.durationNanos()) {
                return new Window(now, durationNanos, 1);
            }
            return new Window(current.startedAtNanos(), current.durationNanos(), current.requestCount() + 1);
        });

        if (updated.requestCount() > maxRequests) {
            long remainingNanos = Math.max(
                    0, updated.startedAtNanos() + updated.durationNanos() - now);
            long retryAfterSeconds = Math.max(
                    1, (remainingNanos + TimeUnit.SECONDS.toNanos(1) - 1) / TimeUnit.SECONDS.toNanos(1));
            throw new TooManyRequestsException(message, retryAfterSeconds);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }

    private record LimitKey(String scope, String clientIp) {
    }

    private record Window(long startedAtNanos, long durationNanos, int requestCount) {
    }
}
