
CREATE TABLE bookings (
                          id CHAR(36) PRIMARY KEY,
                          customer_id CHAR(36) NOT NULL,
                          event_id CHAR(36) NOT NULL,
                          status VARCHAR(30) NOT NULL,
                          total_amount DECIMAL(12, 2) NOT NULL,
                          currency CHAR(3) NOT NULL DEFAULT 'INR',
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE booking_seats (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               booking_id CHAR(36) NOT NULL,
                               seat_id CHAR(36) NOT NULL,
                               price_snapshot DECIMAL(12, 2) NOT NULL,
                               currency CHAR(3) NOT NULL DEFAULT 'INR',

                               CONSTRAINT fk_booking_seats_booking
                                   FOREIGN KEY (booking_id) REFERENCES bookings(id),

                               CONSTRAINT uq_booking_seat
                                   UNIQUE (booking_id, seat_id)
);

CREATE TABLE booking_idempotency (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     customer_id CHAR(36) NOT NULL,
                                     idempotency_key VARCHAR(128) NOT NULL,
                                     request_hash CHAR(64) NOT NULL,
                                     booking_id CHAR(36),
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT uq_customer_idempotency
                                         UNIQUE (customer_id, idempotency_key),

                                     CONSTRAINT fk_idempotency_booking
                                         FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE TABLE booking_sagas (
                               booking_id CHAR(36) PRIMARY KEY,
                               state VARCHAR(40) NOT NULL,
                               last_error TEXT,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                   ON UPDATE CURRENT_TIMESTAMP,

                               CONSTRAINT fk_saga_booking
                                   FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE TABLE outbox_events (
                               id CHAR(36) PRIMARY KEY,
                               aggregate_id CHAR(36) NOT NULL,
                               event_type VARCHAR(100) NOT NULL,
                               payload JSON NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               published_at TIMESTAMP NULL,
                               attempts INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_booking_customer
    ON bookings(customer_id);

CREATE INDEX idx_booking_saga_state
    ON booking_sagas(state);

CREATE INDEX idx_outbox_unpublished
    ON outbox_events(published_at, created_at);
