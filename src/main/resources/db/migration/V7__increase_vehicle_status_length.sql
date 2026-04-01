-- Increase the length of the vehicle status column to accommodate new statuses 
-- like 'RESERVED_PENDING_PAYMENT' (24 chars) which exceeds the original VARCHAR(20)

ALTER TABLE vehicles
    DROP CONSTRAINT chk_vehicle_status,
    ALTER COLUMN status TYPE VARCHAR(50),
    ADD CONSTRAINT chk_vehicle_status CHECK (status IN ('AVAILABLE', 'SOLD', 'IN_MAINTENANCE', 'RESERVED_PENDING_PAYMENT'));
