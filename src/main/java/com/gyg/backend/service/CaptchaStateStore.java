package com.gyg.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CaptchaStateStore {

    private static final int LOCK_STRIPES = 64;

    private final JdbcTemplate jdbcTemplate;

    public CaptchaStateStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public boolean reserveProviderToken(String tokenHash, long expiresAt, long now) {
        lock(tokenHash);
        jdbcTemplate.update(
                "DELETE FROM captcha_used_tokens WHERE token_hash = ? AND expires_at <= ?",
                tokenHash,
                now);
        List<Long> existing = jdbcTemplate.query(
                "SELECT expires_at FROM captcha_used_tokens WHERE token_hash = ?",
                (resultSet, rowNumber) -> resultSet.getLong("expires_at"),
                tokenHash);
        if (!existing.isEmpty()) {
            return false;
        }
        jdbcTemplate.update(
                "INSERT INTO captcha_used_tokens (token_hash, expires_at) VALUES (?, ?)",
                tokenHash,
                expiresAt);
        return true;
    }

    @Transactional
    public void releaseProviderToken(String tokenHash) {
        jdbcTemplate.update("DELETE FROM captcha_used_tokens WHERE token_hash = ?", tokenHash);
    }

    @Transactional
    public void issueTicket(String ticketHash, String bindingHash, long expiresAt) {
        jdbcTemplate.update(
                "INSERT INTO captcha_session_tickets (ticket_hash, binding_hash, expires_at) VALUES (?, ?, ?)",
                ticketHash,
                bindingHash,
                expiresAt);
    }

    @Transactional
    public boolean consumeTicket(String ticketHash, String bindingHash, long now) {
        return jdbcTemplate.update(
                "DELETE FROM captcha_session_tickets "
                        + "WHERE ticket_hash = ? AND binding_hash = ? AND expires_at > ?",
                ticketHash,
                bindingHash,
                now) == 1;
    }

    @Transactional
    public void deleteExpired(long now) {
        jdbcTemplate.update("DELETE FROM captcha_used_tokens WHERE expires_at <= ?", now);
        jdbcTemplate.update("DELETE FROM captcha_session_tickets WHERE expires_at <= ?", now);
    }

    @Scheduled(fixedDelay = 60_000)
    public void cleanupExpired() {
        deleteExpired(System.currentTimeMillis());
    }

    private void lock(String key) {
        int stripe = Math.floorMod(key.hashCode(), LOCK_STRIPES);
        jdbcTemplate.queryForObject(
                "SELECT stripe_id FROM api_rate_limit_locks WHERE stripe_id = ? FOR UPDATE",
                Short.class,
                (short) stripe);
    }
}
