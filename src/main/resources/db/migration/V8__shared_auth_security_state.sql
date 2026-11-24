CREATE TABLE captcha_used_tokens (
    token_hash CHAR(64) PRIMARY KEY,
    expires_at BIGINT NOT NULL
);

CREATE INDEX idx_captcha_used_tokens_expires_at
    ON captcha_used_tokens (expires_at);

CREATE TABLE captcha_session_tickets (
    ticket_hash CHAR(64) PRIMARY KEY,
    binding_hash CHAR(64) NOT NULL,
    expires_at BIGINT NOT NULL
);

CREATE INDEX idx_captcha_session_tickets_expires_at
    ON captcha_session_tickets (expires_at);

CREATE TABLE verified_auth_sessions (
    firebase_uid VARCHAR(255) PRIMARY KEY,
    auth_time BIGINT NOT NULL,
    expires_at BIGINT NOT NULL
);

CREATE INDEX idx_verified_auth_sessions_expires_at
    ON verified_auth_sessions (expires_at);
