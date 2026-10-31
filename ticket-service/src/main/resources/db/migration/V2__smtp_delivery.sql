-- Additive migration: retain V1 and the existing MySQL volume.
ALTER TABLE notification_attempts ADD COLUMN recipient_email VARCHAR(254) NULL;
ALTER TABLE notification_attempts ADD COLUMN delivery_mode VARCHAR(10) NOT NULL DEFAULT 'MOCK';
ALTER TABLE notification_attempts ADD COLUMN claim_token VARCHAR(36) NULL;
ALTER TABLE notification_attempts ADD COLUMN lease_until TIMESTAMP(6) NULL;
ALTER TABLE notification_attempts ADD COLUMN delivery_uncertain BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE notification_attempts ADD COLUMN sent_at TIMESTAMP(6) NULL;
CREATE INDEX ix_notification_claim ON notification_attempts(status,claim_token,created_at);
