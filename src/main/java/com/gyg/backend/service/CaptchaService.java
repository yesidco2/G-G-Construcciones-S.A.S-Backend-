package com.gyg.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CaptchaService {

    private static final int MAX_TICKETS = 100_000;
    private static final int MAX_USED_TOKENS = 100_000;
    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);

    private final CaptchaVerificationClient verificationClient;
    private final int ticketTtlSeconds;
    private final boolean enabled;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Ticket> tickets = new LinkedHashMap<>();
    private final Map<String, Instant> usedTokens = new LinkedHashMap<>();

    @Autowired
    public CaptchaService(
            CaptchaVerificationClient verificationClient,
            @Value("${app.captcha.ticket-ttl-seconds:120}") int ticketTtlSeconds,
            @Value("${app.captcha.enabled:true}") boolean enabled) {
        this(verificationClient, ticketTtlSeconds, enabled, Clock.systemUTC());
    }

    CaptchaService(
            CaptchaVerificationClient verificationClient,
            int ticketTtlSeconds,
            boolean enabled,
            Clock clock) {
        if (ticketTtlSeconds < 1) {
            throw new IllegalArgumentException("El TTL de tickets CAPTCHA debe ser positivo.");
        }
        this.verificationClient = verificationClient;
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
        synchronized (tickets) {
            purgeExpiredTickets();
            if (tickets.size() >= MAX_TICKETS) {
                throw new InvalidCaptchaException();
            }
            tickets.put(ticketHash, new Ticket(bindingHash, expiresAt));
        }
        return new TicketResponse(ticket, ticketTtlSeconds);
    }

    private void verifyToken(String captchaToken, String expectedAction, HttpServletRequest request) {
        if (!enabled || captchaToken == null || captchaToken.isBlank() || captchaToken.length() > 2048) {
            throw new InvalidCaptchaException();
        }

        String tokenHash = sha256(captchaToken);
        Instant expiresAt = clock.instant().plus(TOKEN_TTL);
        synchronized (usedTokens) {
            purgeExpiredTokens();
            if (usedTokens.containsKey(tokenHash) || usedTokens.size() >= MAX_USED_TOKENS) {
                throw new InvalidCaptchaException();
            }
            usedTokens.put(tokenHash, expiresAt);
        }

        try {
            if (!verificationClient.verify(captchaToken, clientIp(request), expectedAction)) {
                throw new InvalidCaptchaException();
            }
        } catch (RuntimeException exception) {
            synchronized (usedTokens) {
                usedTokens.remove(tokenHash, expiresAt);
            }
            throw new InvalidCaptchaException();
        }
    }

    private void purgeExpiredTokens() {
        Instant now = clock.instant();
        Iterator<Instant> iterator = usedTokens.values().iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().isAfter(now)) {
                iterator.remove();
            }
        }
    }

    public boolean consumeTicket(String ticket, HttpServletRequest request) {
        if (ticket == null || ticket.isBlank()) {
            return false;
        }
        String clientIp = clientIp(request);
        Ticket stored;
        synchronized (tickets) {
            stored = tickets.remove(sha256(ticket));
        }
        return stored != null
                && stored.expiresAt().isAfter(clock.instant())
                && MessageDigest.isEqual(
                        stored.bindingHash().getBytes(StandardCharsets.US_ASCII),
                        bindingHash(clientIp, request.getHeader("User-Agent"))
                                .getBytes(StandardCharsets.US_ASCII));
    }

    private void purgeExpiredTickets() {
        Instant now = clock.instant();
        Iterator<Ticket> iterator = tickets.values().iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().expiresAt().isAfter(now)) {
                iterator.remove();
            }
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

    private record Ticket(String bindingHash, Instant expiresAt) {
    }

    public static class InvalidCaptchaException extends RuntimeException {
        public InvalidCaptchaException() {
            super("La verificación CAPTCHA no fue válida.");
        }
    }
}
