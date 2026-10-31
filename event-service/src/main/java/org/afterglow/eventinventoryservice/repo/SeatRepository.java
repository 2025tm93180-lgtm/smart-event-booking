package org.afterglow.eventinventoryservice.repo;

import jakarta.persistence.LockModeType;
import org.afterglow.eventinventoryservice.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, String> {
    List<Seat> findByEventIdOrderBySeatId(String eventId);
    boolean existsByEventIdAndSeatId(String eventId, String seatId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.eventId = :eventId and s.seatId in :seatIds order by s.seatId")
    List<Seat> findForUpdate(@Param("eventId") String eventId, @Param("seatIds") Collection<String> seatIds);

    List<Seat> findByReservationId(String reservationId);
}
