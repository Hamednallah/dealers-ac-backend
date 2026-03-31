-- ═══════════════════════════════════════════════════════
-- V2: Dealers Table
-- ═══════════════════════════════════════════════════════
CREATE TABLE dealers (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         VARCHAR(100) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    subscription_type VARCHAR(20)  NOT NULL DEFAULT 'BASIC',
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT chk_dealer_subscription CHECK (subscription_type IN ('BASIC', 'PREMIUM'))
);

-- Tenant scoping index (most queries filter by tenant_id)
CREATE INDEX idx_dealers_tenant_id ON dealers(tenant_id);

-- Composite: tenant + subscription for PREMIUM filter query
CREATE INDEX idx_dealers_tenant_subscription ON dealers(tenant_id, subscription_type);

-- Unique email per tenant (not globally unique)
CREATE UNIQUE INDEX uq_dealers_email_per_tenant ON dealers(tenant_id, email);
