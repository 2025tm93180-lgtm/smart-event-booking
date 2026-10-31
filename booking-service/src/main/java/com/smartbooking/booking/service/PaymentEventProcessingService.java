
package com.smartbooking.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbooking.booking.dto.PaymentEvent;
import com.smartbooking.booking.entity.Booking;
import com.smartbooking.booking.entity.BookingSaga;
import com.smartbooking.booking.entity.BookingSeat;
import com.smartbooking.booking.entity.OutboxEvent;
import com.smartbooking.booking.repository.BookingRepository;
import com.smartbooking.booking.repository.BookingSagaRepository;
import com.smartbooking.booking.repository.BookingSeatRepository;
import com.smartbooking.booking.repository.OutboxEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentEventProcessingService {

    private final BookingRepository bookingRepository;
    private final BookingSagaRepository bookingSagaRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final InventoryClient inventoryClient;
    private final ObjectMapper objectMapper;

    public PaymentEventProcessingService(
            BookingRepository bookingRepository,
            BookingSagaRepository bookingSagaRepository,
            BookingSeatRepository bookingSeatRepository,
            OutboxEventRepository outboxEventRepository,
            InventoryClient inventoryClient,
            ObjectMapper objectMapper) {
        this.bookingRepository = bookingRepository;
        this.bookingSagaRepository = bookingSagaRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.inventoryClient = inventoryClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void process(PaymentEvent event) {
        if (event == null
                || event.bookingId() == null
                || event.bookingId().isBlank()
                || event.status() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payment event"
            );
        }

        if ("PENDING".equalsIgnoreCase(event.status())) {
            return;
        }

        if (!"CONFIRMED".equalsIgnoreCase(event.status())
                && !"FAILED".equalsIgnoreCase(event.status())
                && !"CANCELLED".equalsIgnoreCase(event.status())) {
            return;
        }

        Booking booking = bookingRepository.findById(event.bookingId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found: " + event.bookingId()
                ));

        BookingSaga saga = bookingSagaRepository.findById(event.bookingId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking Saga not found: " + event.bookingId()
                ));

        if ("CONFIRMED".equalsIgnoreCase(saga.getState())
                || "FAILED".equalsIgnoreCase(saga.getState())
                || "CANCELLED".equalsIgnoreCase(saga.getState())) {
            return;
        }

        if (!"PENDING".equalsIgnoreCase(saga.getState())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Booking Saga is not awaiting payment: " + saga.getState()
            );
        }

        if ("CONFIRMED".equalsIgnoreCase(event.status())) {
            // Confirm inventory first. If this fails, don't issue tickets.
            inventoryClient.confirmReservation(saga.getReservationId());

            booking.setStatus("CONFIRMED");
            saga.setState("CONFIRMED");
            saga.setLastError(null);

            // Persist the ticket-service event in the same DB transaction.
            saveBookingConfirmedEvent(booking);
        } else {
            inventoryClient.releaseReservation(saga.getReservationId());

            booking.setStatus(
                    "CANCELLED".equalsIgnoreCase(event.status())
                            ? "CANCELLED"
                            : "FAILED"
            );
            saga.setState(booking.getStatus());
            saga.setLastError(event.failureReason());
        }

        bookingRepository.save(booking);
        bookingSagaRepository.save(saga);
    }

    private void saveBookingConfirmedEvent(Booking booking) {
        List<String> seatIds = bookingSeatRepository
                .findByBookingId(booking.getId())
                .stream()
                .map(BookingSeat::getSeatId)
                .toList();

        if (seatIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot confirm a booking without seats"
            );
        }

        Instant now = Instant.now();

        Map<String, Object> payload = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "schemaVersion", 1,
                "eventType", "booking.confirmed.v1",
                "correlationId", booking.getId(),
                "occurredAt", now.toString(),
                "bookingId", booking.getId(),
                "payload", Map.of(
                        "customerId", booking.getCustomerId(),
                        "targetEventId", booking.getEventId(),
                        "confirmedAt", now.toString(),
                        "items", seatIds.stream()
                                .map(seatId -> Map.of("seatId", seatId))
                                .toList()
                )
        );

        try {
            String json = objectMapper.writeValueAsString(payload);

            outboxEventRepository.save(
                    new OutboxEvent(
                            booking.getId(),
                            "BOOKING_CONFIRMED",
                            json
                    )
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Failed to serialize booking confirmation event",
                    ex
            );
        }
    }
}
