
package com.smartbooking.booking.service;

import com.smartbooking.booking.dto.BookingResponse;
import com.smartbooking.booking.dto.CreateBookingRequest;
import com.smartbooking.booking.entity.Booking;
import com.smartbooking.booking.entity.BookingIdempotency;
import com.smartbooking.booking.repository.BookingIdempotencyRepository;
import com.smartbooking.booking.repository.BookingRepository;
import com.smartbooking.booking.repository.BookingSeatRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingIdempotencyRepository idempotencyRepository;
    private final BookingCreationTransactionService bookingCreationTransactionService;

    public BookingService(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            BookingIdempotencyRepository idempotencyRepository,
            BookingCreationTransactionService bookingCreationTransactionService) {

        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.bookingCreationTransactionService = bookingCreationTransactionService;
    }

    public BookingResponse createBooking(
            String customerId,
            CreateBookingRequest request,
            String idempotencyKey) {

        if (customerId == null || customerId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Customer ID is required");
        }

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Booking request is required");
        }

        if (request.eventId() == null || request.eventId().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Event ID is required");
        }

        if (request.seatIds() == null || request.seatIds().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "At least one seat is required");
        }

        if (request.seatIds().size() > 10) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A maximum of 10 seats is allowed");
        }

        if (request.seatIds().stream()
                .anyMatch(seatId -> seatId == null || seatId.isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Seat IDs cannot be blank");
        }

        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 128) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid Idempotency-Key is required");
        }

        if (request.seatIds().stream().distinct().count()
                != request.seatIds().size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate seat IDs are not allowed");
        }

        String requestHash = calculateRequestHash(request);

        var existing = idempotencyRepository
                .findByCustomerIdAndIdempotencyKey(
                        customerId, idempotencyKey);

        if (existing.isPresent()) {
            return handleExistingRequest(existing.get(), requestHash);
        }

        try {
            Booking booking = bookingCreationTransactionService.createBooking(
                    customerId,
                    request,
                    idempotencyKey,
                    requestHash
            );

            return toResponse(booking);

        } catch (DataIntegrityViolationException ex) {
            /*
             * The transactional creation call has failed and rolled back.
             * Re-read the idempotency record outside that transaction.
             *
             * A data-integrity error can have causes other than an
             * idempotency-key race, so only handle it as a race if the
             * expected idempotency record now exists.
             */
            var racedRecord = idempotencyRepository
                    .findByCustomerIdAndIdempotencyKey(
                            customerId, idempotencyKey);

            if (racedRecord.isEmpty()) {
                throw ex;
            }

            return handleExistingRequest(racedRecord.get(), requestHash);
        }
    }

    private BookingResponse handleExistingRequest(
            BookingIdempotency record,
            String requestHash) {

        if (!record.getRequestHash().equals(requestHash)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotency key was already used with a different request");
        }

        if (record.getBookingId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Booking request is still being processed");
        }

        Booking previousBooking = bookingRepository
                .findById(record.getBookingId())
                .orElseThrow(() -> new IllegalStateException(
                        "Idempotency record references a missing booking"));

        return toResponse(previousBooking);
    }

    private String calculateRequestHash(CreateBookingRequest request) {
        try {
            String canonicalRequest = request.eventId().trim()
                    + "|"
                    + request.seatIds().stream()
                    .map(String::trim)
                    .sorted(Comparator.naturalOrder())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private BookingResponse toResponse(Booking booking) {
        List<String> seatIds = bookingSeatRepository
                .findByBookingId(booking.getId())
                .stream()
                .map(seat -> seat.getSeatId())
                .toList();

        return new BookingResponse(
                booking.getId(),
                booking.getEventId(),
                seatIds,
                booking.getTotalAmount(),
                booking.getCurrency(),
                booking.getStatus()
        );
    }

    public BookingResponse getBooking(
            String bookingId,
            String customerId) {

        if (bookingId == null || bookingId.isBlank()
                || customerId == null || customerId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking ID and customer ID are required"
            );
        }

        Booking booking = bookingRepository
                .findById(bookingId)
                .filter(b -> b.getCustomerId().equals(customerId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        return toResponse(booking);
    }

    public List<BookingResponse> getCustomerBookings(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Customer ID is required"
            );
        }

        return bookingRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }
}
