
package com.smartbooking.booking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @Column(length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "customer_id", length = 36, nullable = false,
            columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "event_id", length = 36, nullable = false,
            columnDefinition = "CHAR(36)")
    private String eventId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Booking() {
    }

    public Booking(String customerId, String eventId,
                   BigDecimal totalAmount) {
        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.eventId = eventId;
        this.totalAmount = totalAmount;
        this.status = "PENDING";
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
