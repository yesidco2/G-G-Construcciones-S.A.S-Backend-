package com.gyg.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class AuthSecurityStateStoreTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CaptchaStateStore captchaStateStore;

    @Autowired
    private VerifiedSessionStore verifiedSessionStore;

    @Test
    void shouldShareAndAtomicallyConsumeCaptchaTickets() throws Exception {
        CaptchaStateStore secondInstance = new CaptchaStateStore(jdbcTemplate);
        String ticketHash = hash(UUID.randomUUID().toString());
        String bindingHash = hash("ip-and-agent");
        long now = System.currentTimeMillis();
        captchaStateStore.issueTicket(ticketHash, bindingHash, now + Duration.ofMinutes(2).toMillis());

        assertFalse(secondInstance.consumeTicket(ticketHash, hash("different-agent"), now));
        assertTrue(secondInstance.consumeTicket(ticketHash, bindingHash, now));
        assertFalse(captchaStateStore.consumeTicket(ticketHash, bindingHash, now));
    }

    @Test
    void shouldReserveCaptchaProviderTokenOnlyOnceAcrossInstances() throws Exception {
        CaptchaStateStore secondInstance = new CaptchaStateStore(jdbcTemplate);
        String tokenHash = hash(UUID.randomUUID().toString());
        long now = System.currentTimeMillis();

        assertTrue(captchaStateStore.reserveProviderToken(tokenHash, now + 300_000, now));
        assertFalse(secondInstance.reserveProviderToken(tokenHash, now + 300_000, now));
    }

    @Test
    void shouldAllowOnlyOneConcurrentReservationForProviderToken() throws Exception {
        String tokenHash = hash(UUID.randomUUID().toString());
        long now = System.currentTimeMillis();
        int attempts = 20;
        int workers = 8;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int i = 0; i < attempts; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent test did not start.");
                    }
                    return captchaStateStore.reserveProviderToken(tokenHash, now + 300_000, now);
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            long reservations = 0;
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    reservations++;
                }
            }
            assertEquals(1, reservations);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldConsumeCaptchaTicketOnlyOnceUnderConcurrentRequests() throws Exception {
        String ticketHash = hash(UUID.randomUUID().toString());
        String bindingHash = hash("ip-and-agent");
        long now = System.currentTimeMillis();
        captchaStateStore.issueTicket(ticketHash, bindingHash, now + 60_000);
        int attempts = 20;
        int workers = 8;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int i = 0; i < attempts; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent test did not start.");
                    }
                    return new CaptchaStateStore(jdbcTemplate)
                            .consumeTicket(ticketHash, bindingHash, now);
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            long consumed = 0;
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    consumed++;
                }
            }
            assertEquals(1, consumed);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldShareVerifiedSessionsBetweenApplicationInstances() throws Exception {
        VerifiedSessionStore secondInstance = new VerifiedSessionStore(jdbcTemplate);
        String supabaseUid = "test-" + UUID.randomUUID();
        String sessionId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        verifiedSessionStore.save(supabaseUid, sessionId, now + 60_000, now);

        assertTrue(secondInstance.matches(supabaseUid, sessionId, now));
        assertFalse(secondInstance.matches(supabaseUid, UUID.randomUUID().toString(), now));
        assertFalse(secondInstance.matches(supabaseUid, sessionId, now + 60_000));
    }

    private static String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
