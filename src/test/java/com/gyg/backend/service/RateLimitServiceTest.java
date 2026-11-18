package com.gyg.backend.service;

import com.gyg.backend.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class RateLimitServiceTest {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Autowired
    private RateLimitService service;

    @Autowired
    private PersistentRateLimitStore store;

    @Test
    void shouldBlockRequestsAfterLimitAcrossServiceInstances() {
        String scope = "contacto-" + UUID.randomUUID();
        MockHttpServletRequest request = requestFrom("192.0.2.1");

        assertDoesNotThrow(() -> service.check(scope, request, 1, WINDOW, "Limite excedido"));
        RateLimitService otherInstance = new RateLimitService(store);
        TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
                () -> otherInstance.check(scope, request, 1, WINDOW, "Limite excedido"));
        assertTrue(exception.getRetryAfterSeconds() > 0);
    }

    @Test
    void shouldSeparateClientsAndScopes() {
        String scope = "contacto-" + UUID.randomUUID();
        MockHttpServletRequest firstClient = requestFrom("192.0.2.1");
        MockHttpServletRequest secondClient = requestFrom("192.0.2.2");

        assertDoesNotThrow(() -> service.check(scope, firstClient, 1, WINDOW, "Limite excedido"));
        assertDoesNotThrow(() -> service.check(scope, secondClient, 1, WINDOW, "Limite excedido"));
        assertDoesNotThrow(() -> service.check("chat-" + UUID.randomUUID(),
                firstClient, 1, WINDOW, "Limite excedido"));
        assertThrows(TooManyRequestsException.class,
                () -> service.check(scope, firstClient, 1, WINDOW, "Limite excedido"));
    }

    @Test
    void shouldIgnoreForwardedForWhenProxyHeadersAreNotTrusted() {
        String scope = "contacto-" + UUID.randomUUID();
        MockHttpServletRequest firstRequest = requestFrom("192.0.2.1");
        firstRequest.addHeader("X-Forwarded-For", "198.51.100.1");
        MockHttpServletRequest secondRequest = requestFrom("192.0.2.1");
        secondRequest.addHeader("X-Forwarded-For", "203.0.113.8");

        assertDoesNotThrow(() -> service.check(scope, firstRequest, 1, WINDOW, "Limite excedido"));
        assertThrows(TooManyRequestsException.class,
                () -> service.check(scope, secondRequest, 1, WINDOW, "Limite excedido"));
    }

    @Test
    void shouldEnforceLimitForConcurrentRequests() throws Exception {
        String scope = "concurrent-" + UUID.randomUUID();
        int maxRequests = 5;
        int attempts = 24;
        ExecutorService executor = Executors.newFixedThreadPool(6);
        CountDownLatch ready = new CountDownLatch(6);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int i = 0; i < attempts; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent test did not start.");
                    }
                    try {
                        service.check(scope, requestFrom("192.0.2.3"), maxRequests, WINDOW, "Limite excedido");
                        return true;
                    } catch (TooManyRequestsException exception) {
                        return false;
                    }
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            long allowedRequests = 0;
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    allowedRequests++;
                }
            }
            assertEquals(maxRequests, allowedRequests);
        } finally {
            executor.shutdownNow();
        }
    }

    private MockHttpServletRequest requestFrom(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
