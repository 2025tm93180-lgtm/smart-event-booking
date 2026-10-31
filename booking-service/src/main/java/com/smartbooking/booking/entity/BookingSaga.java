
package com.smartbooking.booking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_sagas")
public class BookingSaga {

    @Id
    @Column(name = "booking_id", length = 36,
            columnDefinition = "CHAR(36)")
    private String bookingId;

    @Column(nullable = false, length = 40)
    private String state;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    @Column(name = "reservation_id", length = 36,
            columnDefinition = "CHAR(36)")
    private String reservationId;

    protected BookingSaga() {
    }


    public BookingSaga(
            String bookingId,
            String state,
            String reservationId) {
        this.bookingId = bookingId;
        this.state = state;
        this.reservationId = reservationId;
    }


    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }

    public String getBookingId() { return bookingId; }
    public String getState() { return state; }
    public String getLastError() { return lastError; }

    public void setState(String state) {
        this.state = state;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }


    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

}
