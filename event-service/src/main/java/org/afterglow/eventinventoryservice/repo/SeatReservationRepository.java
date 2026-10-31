package org.afterglow.eventinventoryservice.repo;

import org.afterglow.eventinventoryservice.model.ReservationStatus;
import org.afterglow.eventinventoryservice.model.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, String> {
    Optional<SeatReservation> findByBookingId(String bookingId);
    List<SeatReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);
}
