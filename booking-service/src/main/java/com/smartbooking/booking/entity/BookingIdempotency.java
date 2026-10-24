
package com.smartbooking.booking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "booking_idempotency",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_customer_idempotency",
                columnNames = {"customer_id", "idempotency_key"}
        )
)
public class BookingIdempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 36,
            columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64,
            columnDefinition = "CHAR(64)")
    private String requestHash;

    @Column(name = "booking_id", length = 36,
            columnDefinition = "CHAR(36)")
    private String bookingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected BookingIdempotency() {
    }

    public BookingIdempotency(
            String customerId,
            String idempotencyKey,
            String requestHash) {
        this.customerId = customerId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public String getBookingId() { return bookingId; }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }
}
