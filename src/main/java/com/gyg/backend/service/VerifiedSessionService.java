package com.gyg.backend.service;

import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerifiedSessionService {

    private static final int MAX_SESSIONS = 100_000;
    private final ConcurrentHashMap<String, VerifiedSession> sessions = new ConcurrentHashMap<>();

    public Instant create(FirebaseToken token) {
        Map<String, Object> claims = token.getClaims();
        long authTime = numericClaim(claims, "auth_time");
        long expiration = numericClaim(claims, "exp");
        Instant expiresAt = Instant.ofEpochSecond(expiration);
        if (authTime <= 0 || !expiresAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("El token de Firebase no permite crear una sesión verificada.");
        }
        if (sessions.size() >= MAX_SESSIONS && !sessions.containsKey(token.getUid())) {
            Instant now = Instant.now();
            sessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
            if (sessions.size() >= MAX_SESSIONS) {
                throw new IllegalStateException("No hay capacidad disponible para una sesión verificada.");
            }
        }
        sessions.put(token.getUid(), new VerifiedSession(authTime, expiresAt));
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
        VerifiedSession session = sessions.get(token.getUid());
        if (session == null) {
            return false;
        }
        if (!session.expiresAt().isAfter(Instant.now())) {
            sessions.remove(token.getUid(), session);
            return false;
        }
        return session.authTime() == authTime;
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

    private record VerifiedSession(long authTime, Instant expiresAt) {
    }

    public static final class FirebaseTokenFilterAttribute {
        public static final String NAME = "com.gyg.backend.firebaseToken";

        private FirebaseTokenFilterAttribute() {
        }
    }
}
