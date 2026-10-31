package org.afterglow.eventinventoryservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "seat_reservations", uniqueConstraints = @UniqueConstraint(name = "uk_reservation_booking", columnNames = "booking_id"), indexes = @Index(name = "idx_reservation_expiry", columnList = "status,expires_at"))
public class SeatReservation {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "booking_id", nullable = false, unique = true, length = 36)
    private String bookingId;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "seat_ids", nullable = false, length = 4000)
    private String seatIds;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Version
    private long version;

    protected SeatReservation() {}

    public SeatReservation(String id, String bookingId, String eventId, String seatIds, BigDecimal totalAmount, Instant createdAt, Instant expiresAt) {
        this.id = id; this.bookingId = bookingId; this.eventId = eventId; this.seatIds = seatIds;
        this.totalAmount = totalAmount; this.createdAt = createdAt; this.expiresAt = expiresAt;
        this.status = ReservationStatus.HELD;
    }

    public String getId() { return id; }
    public String getBookingId() { return bookingId; }
    public String getEventId() { return eventId; }
    public String getSeatIds() { return seatIds; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
