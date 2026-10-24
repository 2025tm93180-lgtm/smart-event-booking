
package com.smartbooking.booking.service;

import com.smartbooking.booking.dto.CreateBookingRequest;
import com.smartbooking.booking.entity.Booking;
import com.smartbooking.booking.entity.BookingIdempotency;
import com.smartbooking.booking.entity.BookingSaga;
import com.smartbooking.booking.entity.BookingSeat;
import com.smartbooking.booking.repository.BookingIdempotencyRepository;
import com.smartbooking.booking.repository.BookingRepository;
import com.smartbooking.booking.repository.BookingSagaRepository;
import com.smartbooking.booking.repository.BookingSeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartbooking.booking.entity.OutboxEvent;
import com.smartbooking.booking.repository.OutboxEventRepository;
import java.math.BigDecimal;
import java.util.List;

@Service
public class BookingCreationTransactionService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingSagaRepository bookingSagaRepository;
    private final BookingIdempotencyRepository idempotencyRepository;
    private final OutboxEventRepository outboxEventRepository;

    public BookingCreationTransactionService(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            BookingSagaRepository bookingSagaRepository,
            BookingIdempotencyRepository idempotencyRepository,
            OutboxEventRepository outboxEventRepository) {

        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.bookingSagaRepository = bookingSagaRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public Booking createBooking(
            String customerId,
            CreateBookingRequest request,
            String idempotencyKey,
            String requestHash) {

        Booking booking = new Booking(
                customerId,
                request.eventId(),
                BigDecimal.ZERO
        );

        bookingRepository.save(booking);

        List<BookingSeat> seats = request.seatIds().stream()
                .map(seatId -> new BookingSeat(
                        booking.getId(),
                        seatId,
                        BigDecimal.ZERO
                ))
                .toList();

        bookingSeatRepository.saveAll(seats);

        String payload = """
        {
          "bookingId": "%s",
          "eventId": "%s",
          "seatIds": [%s]
        }
        """.formatted(
                booking.getId(),
                request.eventId(),
                request.seatIds().stream()
                        .map(seatId -> "\"" + seatId + "\"")
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("")
        );

        outboxEventRepository.save(
                new OutboxEvent(
                        booking.getId(),
                        "BOOKING_CREATED",
                        payload
                )
        );

        bookingSagaRepository.save(new BookingSaga(booking.getId(), "PENDING"));

        BookingIdempotency record = new BookingIdempotency(
                customerId,
                idempotencyKey,
                requestHash
        );
        record.setBookingId(booking.getId());

        // Force the unique-key check before this transaction completes.
        idempotencyRepository.saveAndFlush(record);

        return booking;
    }
}
