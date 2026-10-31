package org.afterglow.eventinventoryservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.afterglow.eventinventoryservice.dto.VenueRequest;
import org.afterglow.eventinventoryservice.model.Venue;
import org.afterglow.eventinventoryservice.service.VenueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@CrossOrigin(value = "*")
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping
    public ResponseEntity<Venue> createVenue(
            @RequestBody VenueRequest request) {
        log.info("Received request to create venue: {}", request);
        Venue venue = venueService.createVenue(request);
        log.info("Successfully created venue with ID: {}", venue.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(venue);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Venue> updateVenue(
            @PathVariable String id,
            @RequestBody VenueRequest request) {
        log.info("Received request to update venue with ID: {}, request: {}", id, request);
        Venue venue = venueService.updateVenue(id, request);
        log.info("Successfully updated venue with ID: {}", id);
        return ResponseEntity.ok(venue);
    }

    @GetMapping
    public ResponseEntity<List<Venue>> getAllVenues() {
        log.info("Received request to get all venues");
        List<Venue> venues = venueService.getAllVenues();
        log.info("Fetched {} venues", venues.size());
        return ResponseEntity.ok(venues);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Venue> getVenueById(
            @PathVariable String id) {
        log.info("Received request to get venue with ID: {}", id);
        Venue venue = venueService.getVenueById(id);
        log.info("Fetched venue with ID: {}", id);
        return ResponseEntity.ok(venue);
    }
}