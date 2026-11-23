package com.gyg.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class VerifiedSessionService {

    private final VerifiedSessionStore store;
    private final Clock clock;

    @Autowired
    public VerifiedSessionService(VerifiedSessionStore store) {
        this(store, Clock.systemUTC());
    }

    VerifiedSessionService(VerifiedSessionStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    public static final String IDENTITY_ATTRIBUTE = "com.gyg.backend.authIdentity";

    public Instant create(SupabaseAuth.Identity identity) {
        Instant now = clock.instant();
        if (!identity.expiration().isAfter(now)) {
            throw new IllegalArgumentException("El token de Supabase est? vencido.");
        }
        try {
            store.save(identity.uid(), identity.sessionId(), identity.expiration().toEpochMilli(), now.toEpochMilli());
        } catch (DataAccessException exception) {
            throw new SecurityStateUnavailableException(exception);
        }
        return identity.expiration();
    }

    public boolean isValid(SupabaseAuth.Identity identity) {
        if (identity == null) return false;
        try {
            return store.matches(identity.uid(), identity.sessionId(), Instant.now(clock).toEpochMilli());
        } catch (DataAccessException exception) {
            throw new SecurityStateUnavailableException(exception);
        }
    }

    public static SupabaseAuth.Identity identity(HttpServletRequest request) {
        Object value = request.getAttribute(IDENTITY_ATTRIBUTE);
        return value instanceof SupabaseAuth.Identity identity ? identity : null;
    }
}
