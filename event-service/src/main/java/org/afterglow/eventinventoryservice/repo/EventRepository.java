package org.afterglow.eventinventoryservice.repo;

import org.afterglow.eventinventoryservice.model.Event;
import org.afterglow.eventinventoryservice.model.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, String> {

    List<Event> findByStatus(
            EventStatus status
    );

    boolean existsByVenueId(String venueId);

    boolean existsByVenueIdAndDate(String venueId, LocalDate date);

    boolean existsByVenueIdAndDateAndIdNot(String venueId, LocalDate date, String id);
}