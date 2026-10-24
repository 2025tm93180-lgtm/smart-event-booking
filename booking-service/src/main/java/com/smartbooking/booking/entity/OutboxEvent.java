
package com.smartbooking.booking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @Column(length = 36, columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "aggregate_id", nullable = false, length = 36,
            columnDefinition = "CHAR(36)")
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "json")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "claimed_by", length = 100)
    private String claimedBy;

    @Column(name = "claim_until")
    private LocalDateTime claimUntil;

    protected OutboxEvent() {
    }

    public OutboxEvent(
            String aggregateId,
            String eventType,
            String payload) {
        this.id = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.attempts = 0;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public String getClaimedBy() {
        return claimedBy;
    }

    public LocalDateTime getClaimUntil() {
        return claimUntil;
    }

    public String getId() {
        return id;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public int getAttempts() {
        return attempts;
    }

}
