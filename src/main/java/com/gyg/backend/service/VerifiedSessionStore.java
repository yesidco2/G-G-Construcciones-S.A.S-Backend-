package com.gyg.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerifiedSessionStore {

    private static final int LOCK_STRIPES = 64;

    private final JdbcTemplate jdbcTemplate;

    public VerifiedSessionStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void save(String supabaseUid, String sessionId, long expiresAt, long now) {
        int stripe = Math.floorMod(supabaseUid.hashCode(), LOCK_STRIPES);
        jdbcTemplate.queryForObject(
                "SELECT stripe_id FROM api_rate_limit_locks WHERE stripe_id = ? FOR UPDATE",
                Short.class,
                (short) stripe);
        jdbcTemplate.update("DELETE FROM verified_auth_sessions WHERE expires_at <= ?", now);
        int updated = jdbcTemplate.update(
                "UPDATE verified_auth_sessions SET session_id = ?, expires_at = ? WHERE supabase_uid = ?",
                sessionId,
                expiresAt,
                supabaseUid);
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO verified_auth_sessions (supabase_uid, session_id, expires_at) VALUES (?, ?, ?)",
                    supabaseUid,
                    sessionId,
                    expiresAt);
        }
    }

    public boolean matches(String supabaseUid, String sessionId, long now) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verified_auth_sessions "
                        + "WHERE supabase_uid = ? AND session_id = ? AND expires_at > ?",
                Long.class,
                supabaseUid,
                sessionId,
                now);
        return count != null && count == 1;
    }

    @Transactional
    @Scheduled(fixedDelay = 300_000)
    public void cleanupExpired() {
        jdbcTemplate.update(
                "DELETE FROM verified_auth_sessions WHERE expires_at <= ?",
                System.currentTimeMillis());
    }
}
