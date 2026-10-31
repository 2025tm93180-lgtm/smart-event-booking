
package com.smartbooking.booking.controller;

import com.smartbooking.booking.dto.BookingResponse;
import com.smartbooking.booking.dto.CreateBookingRequest;
import com.smartbooking.booking.service.BookingService;
import com.smartbooking.booking.service.OutboxPublisherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartbooking.booking.entity.OutboxEvent;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final OutboxPublisherService outboxPublisherService;

    public BookingController(
            BookingService bookingService,
            OutboxPublisherService outboxPublisherService) {
        this.bookingService = bookingService;
        this.outboxPublisherService = outboxPublisherService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @RequestHeader("X-Customer-Id") String customerId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateBookingRequest request) {

        BookingResponse response = bookingService.createBooking(
                customerId, request, idempotencyKey);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable String bookingId,
            @RequestHeader("X-Customer-Id") String customerId) {

        BookingResponse response =
                bookingService.getBooking(bookingId, customerId);

        return ResponseEntity.ok(response);
    }


    @GetMapping
    public ResponseEntity<List<BookingResponse>> getCustomerBookings(
            @RequestHeader("X-Customer-Id") String customerId) {

        List<BookingResponse> bookings =
                bookingService.getCustomerBookings(customerId);

        return ResponseEntity.ok(bookings);
    }

//    @GetMapping("/outbox/unpublished")
//    public List<OutboxEvent> getUnpublishedOutboxEvents() {
//        return outboxPublisherService.getUnpublishedEvents();
//    }

}
