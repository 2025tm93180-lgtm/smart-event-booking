package org.afterglow.eventinventoryservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "event_seats", uniqueConstraints = @UniqueConstraint(name = "uk_event_seat", columnNames = {"event_id", "seat_id"}), indexes = @Index(name = "idx_event_seat_status", columnList = "event_id,status"))
public class Seat {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "seat_id", nullable = false, length = 80)
    private String seatId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(name = "reservation_id", length = 36)
    private String reservationId;

    @Version
    private long version;

    protected Seat() {}

    public Seat(String id, String eventId, String seatId, BigDecimal price) {
        this.id = id; this.eventId = eventId; this.seatId = seatId; this.price = price;
        this.status = SeatStatus.AVAILABLE;
    }

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getSeatId() { return seatId; }
    public BigDecimal getPrice() { return price; }
    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus status) { this.status = status; }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
}
