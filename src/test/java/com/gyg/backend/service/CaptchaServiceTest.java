package com.gyg.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CaptchaServiceTest {

    @Test
    void shouldIssueAndConsumeValidSingleUseTicket() {
        CaptchaService service = service(
                (token, ip, action) -> CaptchaVerificationResult.accepted(),
                Clock.systemUTC(),
                new MemoryStore());
        MockHttpServletRequest request = request("192.0.2.10", "Browser A");

        CaptchaService.TicketResponse response = service.verifyAndIssue("provider-token", "login", request);

        assertEquals(120, response.expiresIn());
        assertEquals(43, response.ticket().length());
        assertTrue(service.consumeTicket(response.ticket(), request));
        assertFalse(service.consumeTicket(response.ticket(), request));
    }

    @Test
    void shouldRejectExpiredTicket() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        CaptchaService service = service(
                (token, ip, action) -> CaptchaVerificationResult.accepted(), clock, new MemoryStore());
        MockHttpServletRequest request = request("192.0.2.10", "Browser A");
        String ticket = service.verifyAndIssue("provider-token", "login", request).ticket();
        clock.advanceSeconds(121);

        assertFalse(service.consumeTicket(ticket, request));
    }

    @Test
    void shouldBindTicketToClientIpAndUserAgent() {
        CaptchaService service = service(
                (token, ip, action) -> CaptchaVerificationResult.accepted(),
                Clock.systemUTC(),
                new MemoryStore());
        String ticket = service.verifyAndIssue(
            "provider-token", "login", request("192.0.2.10", "Browser A")).ticket();

        assertFalse(service.consumeTicket(ticket, request("192.0.2.11", "Browser A")));
    }

    @Test
    void shouldBindTicketToUserAgent() {
        CaptchaService service = service(
                (token, ip, action) -> CaptchaVerificationResult.accepted(),
                Clock.systemUTC(),
                new MemoryStore());
        String ticket = service.verifyAndIssue(
            "provider-token", "login", request("192.0.2.10", "Browser A")).ticket();

        assertFalse(service.consumeTicket(ticket, request("192.0.2.10", "Browser B")));
    }

    @Test
    void shouldFailClosedWhenProviderIsUnavailable() {
        CaptchaVerificationClient provider = (token, ip, action) ->
                CaptchaVerificationResult.rejected(CaptchaFailureCode.PROVIDER_TIMEOUT);
        CaptchaService service = service(provider, Clock.systemUTC(), new MemoryStore());

        CaptchaVerificationException exception = assertThrows(CaptchaVerificationException.class,
            () -> service.verifyAndIssue(
                "provider-token", "login", request("192.0.2.10", "Browser A")));
        assertEquals(CaptchaFailureCode.PROVIDER_TIMEOUT, exception.getFailureCode());
    }

    @Test
    void shouldNotIssueTicketWhenProviderRejectsToken() {
        CaptchaService service = service(
                (token, ip, action) -> CaptchaVerificationResult.rejected(CaptchaFailureCode.INVALID_TOKEN),
                Clock.systemUTC(),
                new MemoryStore());

        assertThrows(CaptchaService.InvalidCaptchaException.class,
                () -> service.verifyAndIssue(
                    "provider-token", "login", request("192.0.2.10", "Browser A")));
    }

    @Test
    void shouldVerifySubmissionCaptchaAgainstRemoteAddress() {
        AtomicReference<String> verifiedIp = new AtomicReference<>();
        CaptchaService service = service((token, ip, action) -> {
            verifiedIp.set(ip);
            return CaptchaVerificationResult.accepted();
        }, Clock.systemUTC(), new MemoryStore());
        MockHttpServletRequest request = request("192.0.2.10", "Browser A");
        request.addHeader("X-Forwarded-For", "198.51.100.10");

        service.verifySubmissionToken("provider-token", "newsletter", request);

        assertEquals("192.0.2.10", verifiedIp.get());
    }

    @Test
    void shouldRejectBlankAndOversizedTokensBeforeCallingProvider() {
        CaptchaVerificationClient provider = mock(CaptchaVerificationClient.class);
        when(provider.verify(anyString(), anyString(), anyString()))
                .thenReturn(CaptchaVerificationResult.accepted());
        CaptchaService service = service(provider, Clock.systemUTC(), new MemoryStore());
        MockHttpServletRequest request = request("192.0.2.10", "Browser A");

        assertThrows(CaptchaService.InvalidCaptchaException.class,
                () -> service.verifySubmissionToken("  ", "newsletter", request));
        assertThrows(CaptchaService.InvalidCaptchaException.class,
                () -> service.verifySubmissionToken("x".repeat(2049), "newsletter", request));
        verifyNoInteractions(provider);
    }

    @Test
    void shouldRejectAReusedProviderToken() {
        AtomicInteger calls = new AtomicInteger();
        CaptchaService service = service((token, ip, action) -> {
            calls.incrementAndGet();
            return CaptchaVerificationResult.accepted();
        }, Clock.systemUTC(), new MemoryStore());
        MockHttpServletRequest request = request("192.0.2.10", "Browser A");

        service.verifySubmissionToken("single-use-token", "newsletter", request);

        assertThrows(CaptchaService.InvalidCaptchaException.class,
                () -> service.verifySubmissionToken("single-use-token", "newsletter", request));
        assertEquals(1, calls.get());
    }

    private static CaptchaService service(
            CaptchaVerificationClient provider, Clock clock, CaptchaStateStore store) {
        return new CaptchaService(provider, store, 120, true, clock);
    }

    private static MockHttpServletRequest request(String ip, String userAgent) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        request.addHeader("User-Agent", userAgent);
        return request;
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;

        private MutableClock(Instant instant) {
            this.instant = new AtomicReference<>(instant);
        }

        private void advanceSeconds(long seconds) {
            instant.updateAndGet(value -> value.plusSeconds(seconds));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }

    private static final class MemoryStore extends CaptchaStateStore {
        private final Map<String, Instant> usedTokens = new HashMap<>();
        private final Map<String, Ticket> tickets = new HashMap<>();

        private MemoryStore() {
            super(mock(JdbcTemplate.class));
        }

        @Override
        public synchronized boolean reserveProviderToken(String tokenHash, long expiresAt, long now) {
            usedTokens.entrySet().removeIf(entry -> entry.getValue().toEpochMilli() <= now);
            if (usedTokens.containsKey(tokenHash)) {
                return false;
            }
            usedTokens.put(tokenHash, Instant.ofEpochMilli(expiresAt));
            return true;
        }

        @Override
        public synchronized void releaseProviderToken(String tokenHash) {
            usedTokens.remove(tokenHash);
        }

        @Override
        public synchronized void issueTicket(String ticketHash, String bindingHash, long expiresAt) {
            tickets.put(ticketHash, new Ticket(bindingHash, expiresAt));
        }

        @Override
        public synchronized boolean consumeTicket(String ticketHash, String bindingHash, long now) {
            Ticket ticket = tickets.get(ticketHash);
            if (ticket == null || ticket.expiresAt() <= now || !ticket.bindingHash().equals(bindingHash)) {
                return false;
            }
            tickets.remove(ticketHash);
            return true;
        }

        @Override
        public synchronized void deleteExpired(long now) {
            usedTokens.entrySet().removeIf(entry -> entry.getValue().toEpochMilli() <= now);
            tickets.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        }

        private record Ticket(String bindingHash, long expiresAt) {
        }
    }
}
