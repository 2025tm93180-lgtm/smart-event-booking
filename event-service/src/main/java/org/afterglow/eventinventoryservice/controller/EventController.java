package org.afterglow.eventinventoryservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.afterglow.eventinventoryservice.dto.EventRequest;
import org.afterglow.eventinventoryservice.model.Event;
import org.afterglow.eventinventoryservice.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@CrossOrigin(value = "*")
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<Event> createEvent(
            @RequestBody EventRequest request) {
        log.info("Received request to create event: {}", request);
        Event event = eventService.createEvent(request);
        log.info("Successfully created event with ID: {}", event.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(
            @PathVariable String id,
            @RequestBody EventRequest request) {
        log.info("Received request to update event with ID: {}, request: {}", id, request);
        Event event = eventService.updateEvent(id, request);
        log.info("Successfully updated event with ID: {}", id);
        return ResponseEntity.ok(event);
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        log.info("Received request to get all events");
        List<Event> events = eventService.getAllEvents();
        log.info("Fetched {} events", events.size());
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(
            @PathVariable String id) {
        log.info("Received request to get event with ID: {}", id);
        Event event = eventService.getEventById(id);
        log.info("Fetched event with ID: {}", id);
        return ResponseEntity.ok(event);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable String id) {
        log.info("Received request to delete event with ID: {}", id);
        eventService.deleteEvent(id);
        log.info("Successfully deleted event with ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}