CREATE TABLE api_rate_limit_locks (
    stripe_id SMALLINT PRIMARY KEY
);

INSERT INTO api_rate_limit_locks (stripe_id) VALUES
    (0), (1), (2), (3), (4), (5), (6), (7),
    (8), (9), (10), (11), (12), (13), (14), (15),
    (16), (17), (18), (19), (20), (21), (22), (23),
    (24), (25), (26), (27), (28), (29), (30), (31),
    (32), (33), (34), (35), (36), (37), (38), (39),
    (40), (41), (42), (43), (44), (45), (46), (47),
    (48), (49), (50), (51), (52), (53), (54), (55),
    (56), (57), (58), (59), (60), (61), (62), (63);

CREATE TABLE api_rate_limit_windows (
    scope VARCHAR(200) NOT NULL,
    client_hash CHAR(64) NOT NULL,
    window_started_at BIGINT NOT NULL,
    window_duration BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    request_count BIGINT NOT NULL,
    PRIMARY KEY (scope, client_hash)
);

CREATE INDEX idx_api_rate_limit_windows_expires_at
    ON api_rate_limit_windows (expires_at);
