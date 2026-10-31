
package com.smartbooking.booking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "booking_seats",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_booking_seat",
                columnNames = {"booking_id", "seat_id"}
        )
)
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false, length = 36,
            columnDefinition = "CHAR(36)")
    private String bookingId;

    @Column(name = "seat_id", nullable = false, length = 36,
            columnDefinition = "CHAR(36)")
    private String seatId;

    @Column(name = "price_snapshot", nullable = false,
            precision = 12, scale = 2)
    private BigDecimal priceSnapshot;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    protected BookingSeat() {
    }

    public BookingSeat(
            String bookingId,
            String seatId,
            BigDecimal priceSnapshot) {
        this.bookingId = bookingId;
        this.seatId = seatId;
        this.priceSnapshot = priceSnapshot;
        this.currency = "INR";
    }

    public Long getId() {
        return id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getSeatId() {
        return seatId;
    }

    public BigDecimal getPriceSnapshot() {
        return priceSnapshot;
    }

    public String getCurrency() {
        return currency;
    }
}
