
package com.smartbooking.booking.repository;

import com.smartbooking.booking.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findTop100ByPublishedAtIsNullAndAttemptsLessThanOrderByCreatedAtAsc(
            int maxAttempts
    );

    @Modifying
    @Query("""
    UPDATE OutboxEvent o
    SET o.publishedAt = :publishedAt,
        o.claimedBy = NULL,
        o.claimUntil = NULL
    WHERE o.id = :id
      AND o.publishedAt IS NULL
      AND o.claimedBy = :owner
    """)
    int markAsPublished(
            @Param("id") String id,
            @Param("publishedAt") LocalDateTime publishedAt,
            @Param("owner") String owner
    );

    @Modifying
    @Query("""
    UPDATE OutboxEvent o
    SET o.attempts = o.attempts + 1
    WHERE o.id = :id
    """)
    int incrementAttempts(@Param("id") String id);


    @Modifying
    @Query("""
    UPDATE OutboxEvent o
    SET o.attempts = o.attempts + 1
    WHERE o.id = :id
      AND o.publishedAt IS NULL
      AND o.attempts < :maxAttempts
    """)
    int claimEvent(
            @Param("id") String id,
            @Param("maxAttempts") int maxAttempts
    );


    @Modifying
    @Query("""
    UPDATE OutboxEvent o
    SET o.claimedBy = :owner,
        o.claimUntil = :claimUntil,
        o.attempts = o.attempts + 1
    WHERE o.id = :id
      AND o.publishedAt IS NULL
      AND o.attempts < :maxAttempts
      AND (o.claimUntil IS NULL OR o.claimUntil < :now)
    """)
    int claimEvent(
            @Param("id") String id,
            @Param("owner") String owner,
            @Param("claimUntil") LocalDateTime claimUntil,
            @Param("now") LocalDateTime now,
            @Param("maxAttempts") int maxAttempts
    );

    @Modifying
    @Query("""
    UPDATE OutboxEvent o
    SET o.claimedBy = NULL,
        o.claimUntil = NULL
    WHERE o.id = :id
      AND o.claimedBy = :owner
      AND o.publishedAt IS NULL
    """)
    int releaseClaim(
            @Param("id") String id,
            @Param("owner") String owner
    );

    @Query("""
    SELECT o
    FROM OutboxEvent o
    WHERE o.publishedAt IS NULL
      AND o.attempts < :maxAttempts
      AND (o.claimUntil IS NULL OR o.claimUntil < :now)
    ORDER BY o.createdAt ASC
    """)
    List<OutboxEvent> findAvailableEvents(
            @Param("maxAttempts") int maxAttempts,
            @Param("now") LocalDateTime now
    );
}
