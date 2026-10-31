package org.afterglow.eventinventoryservice.controller;

import jakarta.validation.Valid;
import org.afterglow.eventinventoryservice.dto.CreateReservationRequest;
import org.afterglow.eventinventoryservice.dto.CreateSeatsRequest;
import org.afterglow.eventinventoryservice.model.Seat;
import org.afterglow.eventinventoryservice.model.SeatReservation;
import org.afterglow.eventinventoryservice.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class InventoryController {
    private final InventoryService inventoryService;
    public InventoryController(InventoryService inventoryService) { this.inventoryService = inventoryService; }

    @PostMapping("/api/events/{eventId}/seats")
    public ResponseEntity<List<Seat>> createSeats(@PathVariable String eventId, @Valid @RequestBody CreateSeatsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createSeats(eventId, request));
    }

    @GetMapping("/api/events/{eventId}/seats")
    public List<Seat> listSeats(@PathVariable String eventId) { return inventoryService.listSeats(eventId); }

    @PostMapping("/api/reservations")
    public ResponseEntity<SeatReservation> reserve(@Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.reserve(request));
    }

    @GetMapping("/api/reservations/{reservationId}")
    public SeatReservation getReservation(@PathVariable String reservationId) { return inventoryService.getReservation(reservationId); }

    @PostMapping("/api/reservations/{reservationId}/confirm")
    public SeatReservation confirm(@PathVariable String reservationId) { return inventoryService.confirm(reservationId); }

    @PostMapping("/api/reservations/{reservationId}/release")
    public SeatReservation release(@PathVariable String reservationId) { return inventoryService.release(reservationId); }
}
