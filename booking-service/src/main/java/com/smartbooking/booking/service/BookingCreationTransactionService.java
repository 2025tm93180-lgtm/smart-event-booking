
package com.smartbooking.booking.service;

import com.smartbooking.booking.dto.CreateBookingRequest;
import com.smartbooking.booking.dto.InventoryReservationResponse;
import com.smartbooking.booking.dto.InventorySeatResponse;
import com.smartbooking.booking.entity.Booking;
import com.smartbooking.booking.entity.BookingIdempotency;
import com.smartbooking.booking.entity.BookingSaga;
import com.smartbooking.booking.entity.BookingSeat;
import com.smartbooking.booking.entity.OutboxEvent;
import com.smartbooking.booking.repository.BookingIdempotencyRepository;
import com.smartbooking.booking.repository.BookingRepository;
import com.smartbooking.booking.repository.BookingSagaRepository;
import com.smartbooking.booking.repository.BookingSeatRepository;
import com.smartbooking.booking.repository.OutboxEventRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BookingCreationTransactionService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingSagaRepository bookingSagaRepository;
    private final BookingIdempotencyRepository idempotencyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final InventoryClient inventoryClient;

    public BookingCreationTransactionService(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            BookingSagaRepository bookingSagaRepository,
            BookingIdempotencyRepository idempotencyRepository,
            OutboxEventRepository outboxEventRepository,
            InventoryClient inventoryClient) {

        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.bookingSagaRepository = bookingSagaRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.inventoryClient = inventoryClient;
    }

    @Transactional
    public Booking createBooking(
            String customerId,
            CreateBookingRequest request,
            String idempotencyKey,
            String requestHash) {

        // Fetch current seat information from Event & Inventory Service.
        List<InventorySeatResponse> inventorySeats =
                inventoryClient.getEventSeats(request.eventId());

        Map<String, InventorySeatResponse> seatsById =
                inventorySeats.stream().collect(Collectors.toMap(
                        InventorySeatResponse::seatId,
                        Function.identity(),
                        (first, second) -> first
                ));

        // Validate requested seats and calculate the total.
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (String seatId : request.seatIds()) {
            InventorySeatResponse seat = seatsById.get(seatId);

            if (seat == null || !request.eventId().equals(seat.eventId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Seat " + seatId + " does not exist for event "
                                + request.eventId()
                );
            }

            if (!"AVAILABLE".equalsIgnoreCase(seat.status())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Seat " + seatId + " is not available"
                );
            }

            if (seat.price() == null || seat.price().signum() < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Inventory Service returned an invalid price for seat "
                                + seatId
                );
            }

            totalAmount = totalAmount.add(seat.price());

        }

        Booking booking = new Booking(
                customerId,
                request.eventId(),
                totalAmount
        );

// Atomically hold the requested seats in Inventory Service.
        InventoryReservationResponse reservation =
                inventoryClient.reserveSeats(
                        booking.getId(),
                        request.eventId(),
                        request.seatIds()
                );

// Use the amount confirmed by Inventory Service.
        if (reservation.totalAmount() == null
                || reservation.totalAmount().compareTo(totalAmount) != 0
                || !"HELD".equalsIgnoreCase(reservation.status())) {

            try {
                inventoryClient.releaseReservation(reservation.id());
            } catch (Exception releaseError) {
                // Log this failure in production and reconcile the hold.
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inventory reservation could not be validated"
            );
        }

        totalAmount = reservation.totalAmount();

        bookingRepository.save(booking);

        // Re-create seat snapshots with the generated booking ID.
        List<BookingSeat> persistedSeats = request.seatIds().stream()
                .map(seatId -> new BookingSeat(
                        booking.getId(),
                        seatId,
                        seatsById.get(seatId).price()
                ))
                .toList();

        bookingSeatRepository.saveAll(persistedSeats);

        String payload = """
        {
          "bookingId": "%s",
          "eventId": "%s",
          "seatIds": [%s],
          "totalAmount": %s,
          "currency": "INR"
        }
        """.formatted(
                booking.getId(),
                request.eventId(),
                request.seatIds().stream()
                        .map(seatId -> "\"" + seatId + "\"")
                        .collect(Collectors.joining(", ")),
                totalAmount.toPlainString()
        );

        outboxEventRepository.save(
                new OutboxEvent(
                        booking.getId(),
                        "BOOKING_CREATED",
                        payload
                )
        );

        bookingSagaRepository.save(
                new BookingSaga(booking.getId(), "PENDING", reservation.id()
                )
        );

        BookingIdempotency record = new BookingIdempotency(
                customerId,
                idempotencyKey,
                requestHash
        );

        record.setBookingId(booking.getId());

        // Force the unique-key check before the transaction completes.
        idempotencyRepository.saveAndFlush(record);

        return booking;
    }
}
