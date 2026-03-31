-- ═══════════════════════════════════════════════════════
-- V1: Users Table
-- ═══════════════════════════════════════════════════════
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(100),                         -- NULL for GLOBAL_ADMIN
    username    VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    email       VARCHAR(255),
    role        VARCHAR(30)  NOT NULL DEFAULT 'TENANT_USER',
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_username  ON users(username);
