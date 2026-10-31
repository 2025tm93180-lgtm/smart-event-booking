ALTER TABLE outbox_events
    ADD COLUMN claimed_by VARCHAR(100) NULL,
    ADD COLUMN claim_until DATETIME NULL;