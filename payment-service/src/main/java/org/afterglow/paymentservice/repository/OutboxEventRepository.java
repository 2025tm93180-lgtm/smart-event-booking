package org.afterglow.paymentservice.repository;

import org.afterglow.paymentservice.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    @Query("SELECT o FROM OutboxEvent o WHERE o.published_at IS NULL")
    List<OutboxEvent> findUnpublishedEvents();
}
