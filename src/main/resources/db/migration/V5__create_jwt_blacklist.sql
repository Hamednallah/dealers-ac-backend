-- ═══════════════════════════════════════════════════════
-- V5: JWT Blacklist Table
-- (Used for logout / token revocation)
-- ═══════════════════════════════════════════════════════
CREATE TABLE jwt_blacklist (
    jti        VARCHAR(255) PRIMARY KEY,
    expires_at TIMESTAMP    NOT NULL
);

-- Cleanup index: allows efficient purging of expired tokens
CREATE INDEX idx_jwt_blacklist_expires ON jwt_blacklist(expires_at);
