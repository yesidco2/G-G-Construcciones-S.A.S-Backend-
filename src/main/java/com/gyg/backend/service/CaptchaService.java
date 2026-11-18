package com.gyg.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class CaptchaService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);

    private final CaptchaVerificationClient verificationClient;
    private final CaptchaStateStore stateStore;
    private final int ticketTtlSeconds;
    private final boolean enabled;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public CaptchaService(
            CaptchaVerificationClient verificationClient,
            CaptchaStateStore stateStore,
            @Value("${app.captcha.ticket-ttl-seconds:120}") int ticketTtlSeconds,
            @Value("${app.captcha.enabled:true}") boolean enabled) {
        this(verificationClient, stateStore, ticketTtlSeconds, enabled, Clock.systemUTC());
    }

    CaptchaService(
            CaptchaVerificationClient verificationClient,
            CaptchaStateStore stateStore,
            int ticketTtlSeconds,
            boolean enabled,
            Clock clock) {
        if (ticketTtlSeconds < 1) {
            throw new IllegalArgumentException("El TTL de tickets CAPTCHA debe ser positivo.");
        }
        this.verificationClient = verificationClient;
        this.stateStore = stateStore;
        this.ticketTtlSeconds = ticketTtlSeconds;
        this.enabled = enabled;
        this.clock = clock;
    }

    public void verifySubmissionToken(
            String captchaToken, String expectedAction, HttpServletRequest request) {
        verifyToken(captchaToken, expectedAction, request);
    }

    public TicketResponse verifyAndIssue(
            String captchaToken, String expectedAction, HttpServletRequest request) {
        String clientIp = clientIp(request);
        verifyToken(captchaToken, expectedAction, request);

        byte[] secret = new byte[32];
        secureRandom.nextBytes(secret);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        Instant expiresAt = clock.instant().plusSeconds(ticketTtlSeconds);
        String ticketHash = sha256(ticket);
        String bindingHash = bindingHash(clientIp, request.getHeader("User-Agent"));
        try {
            stateStore.issueTicket(ticketHash, bindingHash, expiresAt.toEpochMilli());
        } catch (DataAccessException exception) {
            throw new CaptchaVerificationException(CaptchaFailureCode.STATE_UNAVAILABLE, exception);
        }
        return new TicketResponse(ticket, ticketTtlSeconds);
    }

    private void verifyToken(String captchaToken, String expectedAction, HttpServletRequest request) {
        if (!enabled) {
            throw new CaptchaVerificationException(CaptchaFailureCode.CONFIGURATION_ERROR);
        }
        if (captchaToken == null || captchaToken.isBlank() || captchaToken.length() > 2048) {
            throw new InvalidCaptchaException();
        }

        String tokenHash = sha256(captchaToken);
        Instant expiresAt = clock.instant().plus(TOKEN_TTL);
        boolean reserved;
        try {
            stateStore.deleteExpired(clock.instant().toEpochMilli());
            reserved = stateStore.reserveProviderToken(
                    tokenHash, expiresAt.toEpochMilli(), clock.instant().toEpochMilli());
        } catch (DataAccessException exception) {
            throw new CaptchaVerificationException(CaptchaFailureCode.STATE_UNAVAILABLE, exception);
        }
        if (!reserved) {
            throw new InvalidCaptchaException(CaptchaFailureCode.TOKEN_REUSED);
        }

        CaptchaVerificationResult result;
        try {
            result = verificationClient.verify(captchaToken, clientIp(request), expectedAction);
        } catch (CaptchaVerificationException exception) {
            if (exception.getFailureCode().retryable()) {
                releaseToken(tokenHash);
            }
            throw exception;
        } catch (RuntimeException exception) {
            releaseToken(tokenHash);
            throw new CaptchaVerificationException(CaptchaFailureCode.PROVIDER_UNAVAILABLE, exception);
        }
        if (result == null || !result.successful()) {
            CaptchaFailureCode code = result == null
                    ? CaptchaFailureCode.PROVIDER_ERROR
                    : result.failureCode();
            if (code == null) {
                code = CaptchaFailureCode.PROVIDER_ERROR;
            }
            if (code.retryable()) {
                releaseToken(tokenHash);
            }
            throw new InvalidCaptchaException(code);
        }
    }

    private void releaseToken(String tokenHash) {
        try {
            stateStore.releaseProviderToken(tokenHash);
        } catch (DataAccessException exception) {
            throw new CaptchaVerificationException(CaptchaFailureCode.STATE_UNAVAILABLE, exception);
        }
    }

    public boolean consumeTicket(String ticket, HttpServletRequest request) {
        if (ticket == null || ticket.isBlank() || ticket.length() > 128) {
            return false;
        }
        String clientIp = clientIp(request);
        try {
            return stateStore.consumeTicket(
                    sha256(ticket),
                    bindingHash(clientIp, request.getHeader("User-Agent")),
                    clock.instant().toEpochMilli());
        } catch (DataAccessException exception) {
            throw new CaptchaVerificationException(CaptchaFailureCode.STATE_UNAVAILABLE, exception);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }

    private static String bindingHash(String ip, String userAgent) {
        String ua = userAgent == null ? "" : userAgent;
        return sha256(ip.length() + ":" + ip + ua.length() + ":" + ua);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible.", exception);
        }
    }

    public record TicketResponse(String ticket, int expiresIn) {
    }

    public static class InvalidCaptchaException extends CaptchaVerificationException {
        public InvalidCaptchaException() {
            this(CaptchaFailureCode.INVALID_TOKEN);
        }

        public InvalidCaptchaException(CaptchaFailureCode failureCode) {
            super(failureCode);
        }
    }
}
