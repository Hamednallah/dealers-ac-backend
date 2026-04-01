-- Add Optimistic Locking version column and reservation expiration timer column
ALTER TABLE vehicles
    ADD COLUMN version INTEGER DEFAULT 0 NOT NULL,
    ADD COLUMN reservation_expires_at TIMESTAMP WITH TIME ZONE NULL;

-- Create an index to quickly sweep the database for expired reservations in the background
CREATE INDEX idx_vehicle_reservations ON vehicles (status, reservation_expires_at) 
WHERE status = 'RESERVED_PENDING_PAYMENT';
