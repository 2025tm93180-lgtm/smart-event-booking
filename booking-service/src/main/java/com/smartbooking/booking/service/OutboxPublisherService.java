
package com.smartbooking.booking.service;

import com.smartbooking.booking.entity.OutboxEvent;
import com.smartbooking.booking.repository.OutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OutboxPublisherService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String BOOKING_TOPIC = "booking-events";
    private static final String BOOKING_CONFIRMED_TOPIC = "booking.confirmed.v1";
    private static final int MAX_ATTEMPTS = 5;
    private static final String PUBLISHER_ID =
            UUID.randomUUID().toString();

    private static final int CLAIM_DURATION_MINUTES = 2;

    public OutboxPublisherService(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> getUnpublishedEvents() {
        return outboxEventRepository.findAvailableEvents(
                MAX_ATTEMPTS,
                LocalDateTime.now()
        );
    }


    @Transactional
    public void markAsPublished(String eventId) {
        int updatedRows = outboxEventRepository.markAsPublished(
                eventId,
                LocalDateTime.now(),
                PUBLISHER_ID
        );

        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "Could not mark outbox event as published. "
                            + "Event may no longer be owned by this publisher: "
                            + eventId
            );
        }
    }


    public void publishEvent(OutboxEvent event) {
        try {
            String topic = "BOOKING_CONFIRMED".equals(event.getEventType())
                    ? BOOKING_CONFIRMED_TOPIC
                    : BOOKING_TOPIC;

            kafkaTemplate.send(
                    topic,
                    event.getAggregateId(),
                    event.getPayload()
            ).get(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            try {
                outboxEventRepository.releaseClaim(
                        event.getId(),
                        PUBLISHER_ID
                );
            } catch (Exception releaseException) {
                e.addSuppressed(releaseException);
            }

            throw new IllegalStateException(
                    "Failed to publish outbox event: " + event.getId(),
                    e
            );
        }

        // Keep this outside the publishing catch block.
        // If this database update fails, don't immediately release the claim.
        markAsPublished(event.getId());
    }


    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<OutboxEvent> events = getUnpublishedEvents();

        for (OutboxEvent event : events) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime claimUntil =
                    now.plusMinutes(CLAIM_DURATION_MINUTES);

            try {
                int claimed = outboxEventRepository.claimEvent(
                        event.getId(),
                        PUBLISHER_ID,
                        claimUntil,
                        now,
                        MAX_ATTEMPTS
                );

                if (claimed == 1) {
                    publishEvent(event);
                }
            } catch (Exception e) {
                System.err.println(
                        "Failed to process outbox event "
                                + event.getId() + ": " + e.getMessage()
                );
            }
        }
    }

}

