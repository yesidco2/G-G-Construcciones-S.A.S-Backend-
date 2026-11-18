package com.gyg.backend.service;

import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

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

    public Instant create(FirebaseToken token) {
        Map<String, Object> claims = token.getClaims();
        long authTime = numericClaim(claims, "auth_time");
        long expiration = numericClaim(claims, "exp");
        Instant expiresAt = Instant.ofEpochSecond(expiration);
        Instant now = clock.instant();
        if (authTime <= 0 || !expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("El token de Firebase no permite crear una sesión verificada.");
        }
        try {
            store.save(token.getUid(), authTime, expiresAt.toEpochMilli(), now.toEpochMilli());
        } catch (DataAccessException exception) {
            throw new SecurityStateUnavailableException(exception);
        }
        return expiresAt;
    }

    public boolean isValid(FirebaseToken token) {
        if (token == null) {
            return false;
        }
        long authTime;
        try {
            authTime = numericClaim(token.getClaims(), "auth_time");
        } catch (IllegalArgumentException exception) {
            return false;
        }
        try {
            return store.matches(token.getUid(), authTime, Instant.now(clock).toEpochMilli());
        } catch (DataAccessException exception) {
            throw new SecurityStateUnavailableException(exception);
        }
    }

    public static FirebaseToken firebaseToken(HttpServletRequest request) {
        Object value = request.getAttribute(FirebaseTokenFilterAttribute.NAME);
        return value instanceof FirebaseToken token ? token : null;
    }

    private static long numericClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("Falta un claim requerido del token de Firebase.");
    }

    public static final class FirebaseTokenFilterAttribute {
        public static final String NAME = "com.gyg.backend.firebaseToken";

        private FirebaseTokenFilterAttribute() {
        }
    }
}
