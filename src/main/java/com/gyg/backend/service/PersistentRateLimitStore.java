package com.gyg.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Service
public class PersistentRateLimitStore {

    private static final int LOCK_STRIPES = 64;

    private final JdbcTemplate jdbcTemplate;

    public PersistentRateLimitStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Window increment(String scope, String clientHash, long durationMillis) {
        // A shared stripe lock serializes updates to the same key across application instances.
        int stripe = Math.floorMod(31 * scope.hashCode() + clientHash.hashCode(), LOCK_STRIPES);
        jdbcTemplate.queryForObject(
                "SELECT stripe_id FROM api_rate_limit_locks WHERE stripe_id = ? FOR UPDATE",
                Short.class,
                (short) stripe);
        long nowMillis = jdbcTemplate.queryForObject(
                "SELECT CURRENT_TIMESTAMP", Timestamp.class).getTime();

        List<Window> windows = jdbcTemplate.query(
                "SELECT window_started_at, window_duration, expires_at, request_count "
                        + "FROM api_rate_limit_windows WHERE scope = ? AND client_hash = ?",
                (resultSet, rowNumber) -> new Window(
                        resultSet.getLong("window_started_at"),
                        resultSet.getLong("window_duration"),
                        resultSet.getLong("expires_at"),
                        resultSet.getLong("request_count"),
                        nowMillis),
                scope,
                clientHash);

        if (windows.isEmpty()) {
            long expiresAt = Math.addExact(nowMillis, durationMillis);
            jdbcTemplate.update(
                    "INSERT INTO api_rate_limit_windows "
                            + "(scope, client_hash, window_started_at, window_duration, expires_at, request_count) "
                            + "VALUES (?, ?, ?, ?, ?, 1)",
                    scope,
                    clientHash,
                    nowMillis,
                    durationMillis,
                    expiresAt);
            return new Window(nowMillis, durationMillis, expiresAt, 1, nowMillis);
        }

        Window current = windows.get(0);
        if (nowMillis - current.startedAt() >= current.durationMillis()) {
            long expiresAt = Math.addExact(nowMillis, durationMillis);
            jdbcTemplate.update(
                    "UPDATE api_rate_limit_windows SET window_started_at = ?, window_duration = ?, "
                            + "expires_at = ?, request_count = 1 WHERE scope = ? AND client_hash = ?",
                    nowMillis,
                    durationMillis,
                    expiresAt,
                    scope,
                    clientHash);
            return new Window(nowMillis, durationMillis, expiresAt, 1, nowMillis);
        }

        long requestCount = current.requestCount() + 1;
        jdbcTemplate.update(
                "UPDATE api_rate_limit_windows SET request_count = ? WHERE scope = ? AND client_hash = ?",
                requestCount,
                scope,
                clientHash);
        return new Window(
                current.startedAt(), current.durationMillis(), current.expiresAt(), requestCount, nowMillis);
    }

    @Transactional
    public void deleteExpiredBefore(long retentionMillis) {
        long cutoffMillis = jdbcTemplate.queryForObject(
                "SELECT CURRENT_TIMESTAMP", Timestamp.class).getTime() - retentionMillis;
        jdbcTemplate.update(
                "DELETE FROM api_rate_limit_windows WHERE expires_at < ?",
                cutoffMillis);
    }

    public record Window(
            long startedAt, long durationMillis, long expiresAt, long requestCount, long nowMillis) {
    }
}
