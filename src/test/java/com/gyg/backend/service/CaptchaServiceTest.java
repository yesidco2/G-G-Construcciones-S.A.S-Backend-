package com.gyg.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CaptchaServiceTest {

    @Test
    void shouldIssueAndConsumeValidSingleUseTicket() {
        CaptchaService service = service((token, ip, action) -> true, Clock.systemUTC());
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
        CaptchaService service = service((token, ip, action) -> true, clock);
        MockHttpServletRequest request = request("192.0.2.10", "Browser A");
        String ticket = service.verifyAndIssue("provider-token", "login", request).ticket();
        clock.advanceSeconds(121);

        assertFalse(service.consumeTicket(ticket, request));
    }

    @Test
    void shouldBindTicketToClientIpAndUserAgent() {
        CaptchaService service = service((token, ip, action) -> true, Clock.systemUTC());
        String ticket = service.verifyAndIssue(
            "provider-token", "login", request("192.0.2.10", "Browser A")).ticket();

        assertFalse(service.consumeTicket(ticket, request("192.0.2.11", "Browser A")));
    }

    @Test
    void shouldBindTicketToUserAgent() {
        CaptchaService service = service((token, ip, action) -> true, Clock.systemUTC());
        String ticket = service.verifyAndIssue(
            "provider-token", "login", request("192.0.2.10", "Browser A")).ticket();

        assertFalse(service.consumeTicket(ticket, request("192.0.2.10", "Browser B")));
    }

    @Test
    void shouldFailClosedWhenProviderIsUnavailable() {
        CaptchaVerificationClient provider = (token, ip, action) -> {
            throw new CaptchaProviderUnavailableException();
        };
        CaptchaService service = service(provider, Clock.systemUTC());

        assertThrows(CaptchaService.InvalidCaptchaException.class,
            () -> service.verifyAndIssue(
                "provider-token", "login", request("192.0.2.10", "Browser A")));
    }

    @Test
    void shouldNotIssueTicketWhenProviderRejectsToken() {
        CaptchaService service = service((token, ip, action) -> false, Clock.systemUTC());

        assertThrows(CaptchaService.InvalidCaptchaException.class,
                () -> service.verifyAndIssue(
                    "provider-token", "login", request("192.0.2.10", "Browser A")));
    }

    @Test
    void shouldVerifySubmissionCaptchaAgainstRemoteAddress() {
        AtomicReference<String> verifiedIp = new AtomicReference<>();
        CaptchaService service = service((token, ip, action) -> {
            verifiedIp.set(ip);
            return true;
        }, Clock.systemUTC());
        MockHttpServletRequest request = request("192.0.2.10", "Browser A");
        request.addHeader("X-Forwarded-For", "198.51.100.10");

        service.verifySubmissionToken("provider-token", "contacto", request);

        assertEquals("192.0.2.10", verifiedIp.get());
    }

    @Test
    void shouldRejectBlankAndOversizedTokensBeforeCallingProvider() {
        CaptchaVerificationClient provider = mock(CaptchaVerificationClient.class);
        when(provider.verify(anyString(), anyString(), anyString())).thenReturn(true);
        CaptchaService service = service(provider, Clock.systemUTC());
        MockHttpServletRequest request = request("192.0.2.10", "Browser A");

        assertThrows(CaptchaService.InvalidCaptchaException.class,
                () -> service.verifySubmissionToken("  ", "contacto", request));
        assertThrows(CaptchaService.InvalidCaptchaException.class,
                () -> service.verifySubmissionToken("x".repeat(2049), "contacto", request));
        verifyNoInteractions(provider);
    }

    @Test
    void shouldRejectAReusedProviderToken() {
        AtomicInteger calls = new AtomicInteger();
        CaptchaService service = service((token, ip, action) -> {
            calls.incrementAndGet();
            return true;
        }, Clock.systemUTC());
        MockHttpServletRequest request = request("192.0.2.10", "Browser A");

        service.verifySubmissionToken("single-use-token", "contacto", request);

        assertThrows(CaptchaService.InvalidCaptchaException.class,
                () -> service.verifySubmissionToken("single-use-token", "contacto", request));
        assertEquals(1, calls.get());
    }

    private static CaptchaService service(CaptchaVerificationClient provider, Clock clock) {
        return new CaptchaService(provider, 120, true, clock);
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
}
