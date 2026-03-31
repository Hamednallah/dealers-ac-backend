-- ═══════════════════════════════════════════════════════
-- V3: Vehicles Table
-- ═══════════════════════════════════════════════════════
CREATE TABLE vehicles (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(100)   NOT NULL,
    dealer_id   UUID           NOT NULL REFERENCES dealers(id) ON DELETE CASCADE,
    model       VARCHAR(255)   NOT NULL,
    price       DECIMAL(12, 2) NOT NULL CHECK (price > 0),
    status      VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE',
    created_at  TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP      NOT NULL DEFAULT now(),

    CONSTRAINT chk_vehicle_status CHECK (status IN ('AVAILABLE', 'SOLD'))
);

-- Tenant scoping (all vehicle queries are tenant-scoped)
CREATE INDEX idx_vehicles_tenant_id ON vehicles(tenant_id);

-- FK lookup
CREATE INDEX idx_vehicles_dealer_id ON vehicles(dealer_id);

-- Composite: tenant + status for status filter
CREATE INDEX idx_vehicles_tenant_status ON vehicles(tenant_id, status);

-- Composite: tenant + price range for min/max filter
CREATE INDEX idx_vehicles_tenant_price ON vehicles(tenant_id, price);
