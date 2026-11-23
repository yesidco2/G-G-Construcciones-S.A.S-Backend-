package com.gyg.backend.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VerifiedSessionServiceTest {
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final VerifiedSessionStore store = mock(VerifiedSessionStore.class);
    private final VerifiedSessionService service = new VerifiedSessionService(store, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void bindsCaptchaToTheSupabaseSession() {
        var expiration = now.plusSeconds(100);
        var identity = new SupabaseAuth.Identity("uid", "user@example.com", true, "session", expiration);
        assertEquals(expiration, service.create(identity));
        verify(store).save("uid", "session", expiration.toEpochMilli(), now.toEpochMilli());
        when(store.matches("uid", "session", now.toEpochMilli())).thenReturn(true);
        assertTrue(service.isValid(identity));
        assertFalse(service.isValid(new SupabaseAuth.Identity("uid", "user@example.com", true, "another", expiration)));
    }

    @Test
    void rejectsExpiredTokens() {
        var identity = new SupabaseAuth.Identity("uid", "user@example.com", true, "session", now);
        assertThrows(IllegalArgumentException.class, () -> service.create(identity));
        verifyNoInteractions(store);
    }
}
