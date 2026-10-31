package org.afterglow.eventinventoryservice.repo;

import jakarta.persistence.LockModeType;
import org.afterglow.eventinventoryservice.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VenueRepository extends JpaRepository<Venue, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Venue v WHERE v.id = :id")
    Optional<Venue> findByIdForUpdate(@Param("id") String id);
}