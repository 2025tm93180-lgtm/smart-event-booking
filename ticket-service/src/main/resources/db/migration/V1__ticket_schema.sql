CREATE TABLE booking_issuances (
  booking_id VARCHAR(100) PRIMARY KEY,
  customer_id VARCHAR(100) NOT NULL,
  event_id BIGINT NOT NULL,
  contract_hash CHAR(64) NOT NULL,
  issued_at TIMESTAMP(6) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE TABLE tickets (
  id VARCHAR(36) PRIMARY KEY,
  booking_id VARCHAR(100) NOT NULL,
  customer_id VARCHAR(100) NOT NULL,
  event_id BIGINT NOT NULL,
  seat_id BIGINT NOT NULL,
  ticket_code VARCHAR(40) NOT NULL UNIQUE,
  status VARCHAR(20) NOT NULL,
  issued_at TIMESTAMP(6) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT uq_ticket_booking_seat UNIQUE (booking_id, seat_id),
  CONSTRAINT fk_ticket_issuance FOREIGN KEY (booking_id) REFERENCES booking_issuances(booking_id),
  CONSTRAINT ck_ticket_status CHECK (status IN ('ISSUED','CANCELLED'))
);
CREATE INDEX ix_tickets_customer ON tickets(customer_id);
CREATE TABLE notification_attempts (
  id VARCHAR(36) PRIMARY KEY,
  booking_id VARCHAR(100) NOT NULL,
  ticket_id VARCHAR(36) NOT NULL,
  correlation_id VARCHAR(100) NOT NULL,
  channel VARCHAR(10) NOT NULL,
  status VARCHAR(20) NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  idempotency_key VARCHAR(100) NOT NULL UNIQUE,
  last_error VARCHAR(300) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_notification_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id),
  CONSTRAINT ck_notification_status CHECK (status IN ('PENDING','SENT','FAILED')),
  CONSTRAINT ck_notification_channel CHECK (channel IN ('EMAIL','SMS')),
  CONSTRAINT ck_attempt_count CHECK (attempt_count >= 0)
);
CREATE INDEX ix_notifications_booking_status ON notification_attempts(booking_id,status);
CREATE INDEX ix_notifications_pending ON notification_attempts(status,created_at,id);
CREATE TABLE inbox_messages (
  message_id VARCHAR(100) PRIMARY KEY,
  message_type VARCHAR(100) NOT NULL,
  contract_hash CHAR(64) NOT NULL,
  processed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
