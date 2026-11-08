package com.gyg.backend.service;

import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VerifiedSessionServiceTest {

    @Test
    void shouldValidateSessionOnlyForMatchingUidAndAuthTime() {
        VerifiedSessionService sessions = new VerifiedSessionService();
        FirebaseToken token = token("uid-1", 1_800_000_000L, 1_800_000_100L);
        sessions.create(token);

        assertTrue(sessions.isValid(token));
        assertFalse(sessions.isValid(token("uid-2", 1_800_000_000L, 1_800_000_100L)));
        assertFalse(sessions.isValid(token("uid-1", 1_800_000_001L, 1_800_000_100L)));
    }

    @Test
    void shouldRejectExpiredFirebaseTokenWhenCreatingSession() {
        VerifiedSessionService sessions = new VerifiedSessionService();
        FirebaseToken token = token("uid-1", 1_800_000_000L, Instant.now().minusSeconds(1).getEpochSecond());

        assertThrows(IllegalArgumentException.class, () -> sessions.create(token));
    }

    private static FirebaseToken token(String uid, long authTime, long expiration) {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn(uid);
        when(token.getClaims()).thenReturn(Map.of("auth_time", authTime, "exp", expiration));
        return token;
    }
}
