package org.afterglow.paymentservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.afterglow.paymentservice.dto.BookingEvent;
import org.afterglow.paymentservice.service.BookingEventProcessingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BookingEventListener {

    private final ObjectMapper objectMapper;
    private final BookingEventProcessingService processingService;

    @Autowired
    public BookingEventListener(ObjectMapper objectMapper,
                                BookingEventProcessingService processingService) {
        this.objectMapper = objectMapper;
        this.processingService = processingService;
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.booking-events:booking-events}",
            groupId = "${spring.kafka.consumer.group-id:payment-service}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        if (record == null) {
            log.warn("Received null ConsumerRecord");
            return;
        }

        String key = record.key();
        String payload = record.value();
        log.info("Received booking event with key: {} and payload: {}", key, payload);

        processPayload(payload, key);
    }

    public void consume(String message) {
        log.info("Received booking event message: {}", message);
        processPayload(message, null);
    }

    private void processPayload(String payload, String key) {
        if (payload == null || payload.isBlank()) {
            log.warn("Received empty or blank booking event payload for key: {}", key);
            return;
        }

        try {
            BookingEvent event = objectMapper.readValue(payload, BookingEvent.class);
            if ((event.getBookingId() == null || event.getBookingId().isBlank()) && key != null) {
                event.setBookingId(key);
            }
            processingService.process(event, key);
            log.info("Successfully processed booking event for bookingId: {}", event.getBookingId());
        } catch (Exception e) {
            log.error("Failed to parse and process booking event: {}", payload, e);
            throw new RuntimeException("Error processing booking event", e);
        }
    }
}
