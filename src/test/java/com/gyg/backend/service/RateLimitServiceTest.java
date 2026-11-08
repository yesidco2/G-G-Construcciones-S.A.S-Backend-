package com.gyg.backend.service;

import com.gyg.backend.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimitServiceTest {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Test
    void shouldBlockRequestsAfterLimit() {
        RateLimitService service = new RateLimitService();
        MockHttpServletRequest request = requestFrom("192.0.2.1");

        assertDoesNotThrow(() -> service.check("contacto", request, 1, WINDOW, "Limite excedido"));
        TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
                () -> service.check("contacto", request, 1, WINDOW, "Limite excedido"));
        org.junit.jupiter.api.Assertions.assertTrue(exception.getRetryAfterSeconds() > 0);
    }

    @Test
    void shouldSeparateClientsAndScopes() {
        RateLimitService service = new RateLimitService();
        MockHttpServletRequest firstClient = requestFrom("192.0.2.1");
        MockHttpServletRequest secondClient = requestFrom("192.0.2.2");

        assertDoesNotThrow(() -> service.check("contacto", firstClient, 1, WINDOW, "Limite excedido"));
        assertDoesNotThrow(() -> service.check("contacto", secondClient, 1, WINDOW, "Limite excedido"));
        assertDoesNotThrow(() -> service.check("chat", firstClient, 1, WINDOW, "Limite excedido"));
        assertThrows(TooManyRequestsException.class,
                () -> service.check("contacto", firstClient, 1, WINDOW, "Limite excedido"));
    }

    @Test
    void shouldIgnoreForwardedForWhenProxyHeadersAreNotTrusted() {
        RateLimitService service = new RateLimitService();
        MockHttpServletRequest firstRequest = requestFrom("192.0.2.1");
        firstRequest.addHeader("X-Forwarded-For", "198.51.100.1");
        MockHttpServletRequest secondRequest = requestFrom("192.0.2.1");
        secondRequest.addHeader("X-Forwarded-For", "203.0.113.8");

        assertDoesNotThrow(() -> service.check("contacto", firstRequest, 1, WINDOW, "Limite excedido"));
        assertThrows(TooManyRequestsException.class,
                () -> service.check("contacto", secondRequest, 1, WINDOW, "Limite excedido"));
    }

    private MockHttpServletRequest requestFrom(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
