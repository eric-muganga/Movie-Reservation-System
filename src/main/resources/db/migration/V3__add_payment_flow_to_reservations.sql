
ALTER TABLE reservations
    ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN payment_reference VARCHAR(100),
    ADD COLUMN payment_expires_at TIMESTAMPTZ,
    ADD COLUMN paid_at TIMESTAMPTZ;

CREATE INDEX idx_reservations_payment_status
    ON reservations (payment_status);

CREATE INDEX idx_reservations_payment_expires_at
    ON reservations (payment_expires_at);