package com.gyg.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CaptchaServiceTest {

    @Test
    void shouldIssueAndConsumeValidSingleUseTicket() {
        CaptchaService service = service((token, ip) -> true, Clock.systemUTC());
        MockHttpServletRequest request = request("192.0.2.10", "Browser A");

        CaptchaService.TicketResponse response = service.verifyAndIssue("provider-token", request);

        assertEquals(120, response.expiresIn());
        assertEquals(43, response.ticket().length());
        assertTrue(service.consumeTicket(response.ticket(), request));
        assertFalse(service.consumeTicket(response.ticket(), request));
    }

    @Test
    void shouldRejectExpiredTicket() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        CaptchaService service = service((token, ip) -> true, clock);
        MockHttpServletRequest request = request("192.0.2.10", "Browser A");
        String ticket = service.verifyAndIssue("provider-token", request).ticket();
        clock.advanceSeconds(121);

        assertFalse(service.consumeTicket(ticket, request));
    }

    @Test
    void shouldBindTicketToClientIpAndUserAgent() {
        CaptchaService service = service((token, ip) -> true, Clock.systemUTC());
        String ticket = service.verifyAndIssue("provider-token", request("192.0.2.10", "Browser A")).ticket();

        assertFalse(service.consumeTicket(ticket, request("192.0.2.11", "Browser A")));
    }

    @Test
    void shouldBindTicketToUserAgent() {
        CaptchaService service = service((token, ip) -> true, Clock.systemUTC());
        String ticket = service.verifyAndIssue("provider-token", request("192.0.2.10", "Browser A")).ticket();

        assertFalse(service.consumeTicket(ticket, request("192.0.2.10", "Browser B")));
    }

    @Test
    void shouldFailClosedWhenProviderIsUnavailable() {
        CaptchaVerificationClient provider = (token, ip) -> {
            throw new CaptchaProviderUnavailableException();
        };
        CaptchaService service = service(provider, Clock.systemUTC());

        assertThrows(CaptchaProviderUnavailableException.class,
                () -> service.verifyAndIssue("provider-token", request("192.0.2.10", "Browser A")));
    }

    @Test
    void shouldNotIssueTicketWhenProviderRejectsToken() {
        CaptchaService service = service((token, ip) -> false, Clock.systemUTC());

        assertThrows(CaptchaService.InvalidCaptchaException.class,
                () -> service.verifyAndIssue("provider-token", request("192.0.2.10", "Browser A")));
    }

    @Test
    void shouldVerifySubmissionCaptchaAgainstRemoteAddress() {
        AtomicReference<String> verifiedIp = new AtomicReference<>();
        CaptchaService service = service((token, ip) -> {
            verifiedIp.set(ip);
            return true;
        }, Clock.systemUTC());
        MockHttpServletRequest request = request("192.0.2.10", "Browser A");
        request.addHeader("X-Forwarded-For", "198.51.100.10");

        service.verifySubmissionToken("provider-token", request);

        assertEquals("192.0.2.10", verifiedIp.get());
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
