package com.smartbooking.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbooking.booking.dto.PaymentEvent;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    private final ObjectMapper objectMapper;
    private final PaymentEventProcessingService processingService;

    public PaymentEventListener(
            ObjectMapper objectMapper,
            PaymentEventProcessingService processingService) {
        this.objectMapper = objectMapper;
        this.processingService = processingService;
    }


    @KafkaListener(
            topics = "${booking.kafka.payment-events-topic:payment-events}",
            groupId = "${spring.kafka.consumer.group-id:booking-service}"
    )
    public void consume(String message) throws Exception {
        System.out.println(">>> PAYMENT EVENT RECEIVED: " + message);

        PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);

        System.out.println(">>> PAYMENT STATUS: " + event.status()
                + ", BOOKING ID: " + event.bookingId());

        processingService.process(event);

        System.out.println(">>> PAYMENT EVENT PROCESSED SUCCESSFULLY");
    }

}