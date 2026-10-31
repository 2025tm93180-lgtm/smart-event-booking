package org.afterglow.eventinventoryservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.afterglow.eventinventoryservice.dto.EventRequest;
import org.afterglow.eventinventoryservice.exception.EventException;
import org.afterglow.eventinventoryservice.exception.VenueException;
import org.afterglow.eventinventoryservice.model.Event;
import org.afterglow.eventinventoryservice.model.Venue;
import org.afterglow.eventinventoryservice.repo.EventRepository;
import org.afterglow.eventinventoryservice.repo.VenueRepository;
import org.afterglow.eventinventoryservice.service.EventService;
import org.afterglow.eventinventoryservice.util.ValidationUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;

    public EventServiceImpl(
            EventRepository eventRepository,
            VenueRepository venueRepository) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
    }

    @Override
    @Transactional
    public Event createEvent(EventRequest request) {
        log.info("Creating event with title: {} and venue ID: {}", request.title(), request.venueId());
        ValidationUtil.validateEvent(request);

        Venue venue = getActiveVenue(request.venueId());
        ValidationUtil.validateEventVenueCapacity(request, venue);

        if (eventRepository.existsByVenueIdAndDate(request.venueId(), request.date())) {
            log.error("An event is already scheduled at venue ID: {} on date: {}", request.venueId(), request.date());
            throw new EventException(
                "An event is already scheduled at this venue on " + request.date(),
                HttpStatus.BAD_REQUEST
            );
        }

        Event event = new Event();
        applyRequest(event, request);

        try {
            Event savedEvent = eventRepository.saveAndFlush(event);
            log.info("Created event successfully with ID: {}", savedEvent.getId());
            return savedEvent;
        } catch (DataIntegrityViolationException ex) {
            log.error("Concurrent conflict: Event already scheduled for venue ID {} on date {}", request.venueId(), request.date(), ex);
            throw new EventException(
                "An event is already scheduled at this venue on " + request.date(),
                HttpStatus.BAD_REQUEST,
                ex
            );
        }
    }

    @Override
    @Transactional
    public Event updateEvent(String id, EventRequest request) {
        log.info("Updating event with ID: {}", id);
        ValidationUtil.validateEvent(request);

        Event event = eventRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Event not found with ID: {}", id);
                return new EventException(
                    "Event not found with ID: " + id,
                    HttpStatus.NOT_FOUND
                );
            });

        Venue venue = getActiveVenue(request.venueId());
        ValidationUtil.validateEventVenueCapacity(request, venue);

        if (eventRepository.existsByVenueIdAndDateAndIdNot(request.venueId(), request.date(), id)) {
            log.error("An event is already scheduled at venue ID: {} on date: {} for another event", request.venueId(), request.date());
            throw new EventException(
                "An event is already scheduled at this venue on " + request.date(),
                HttpStatus.BAD_REQUEST
            );
        }

        applyRequest(event, request);
        try {
            Event savedEvent = eventRepository.saveAndFlush(event);
            log.info("Updated event successfully with ID: {}", savedEvent.getId());
            return savedEvent;
        } catch (DataIntegrityViolationException ex) {
            log.error("Concurrent conflict: Event already scheduled for venue ID {} on date {}", request.venueId(), request.date(), ex);
            throw new EventException(
                "An event is already scheduled at this venue on " + request.date(),
                HttpStatus.BAD_REQUEST,
                ex
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> getAllEvents() {
        log.info("Fetching all events");
        List<Event> events = eventRepository.findAll();
        log.info("Fetched {} events", events.size());
        return events;
    }

    @Override
    @Transactional(readOnly = true)
    public Event getEventById(String id) {
        log.info("Fetching event with ID: {}", id);
        return eventRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Event not found with ID: {}", id);
                return new EventException(
                    "Event not found with ID: " + id,
                    HttpStatus.NOT_FOUND
                );
            });
    }

    @Override
    @Transactional
    public void deleteEvent(String id) {
        log.info("Deleting event with ID: {}", id);
        Event event = eventRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Event not found with ID: {}", id);
                return new EventException(
                    "Event not found with ID: " + id,
                    HttpStatus.NOT_FOUND
                );
            });

        eventRepository.delete(event);
        log.info("Deleted event successfully with ID: {}", id);
    }

    private Venue getActiveVenue(String venueId) {
        log.debug("Validating active venue with ID: {}", venueId);
        Venue venue = venueRepository.findByIdForUpdate(venueId)
            .orElseThrow(() -> {
                log.error("Venue not found with ID: {}", venueId);
                return new VenueException(
                    "Venue not found with ID: " + venueId,
                    HttpStatus.NOT_FOUND
                );
            });

        if (!venue.isActive()) {
            log.error("Venue with ID: {} is inactive", venueId);
            throw new VenueException(
                "Cannot create or update an event for an inactive venue",
                HttpStatus.BAD_REQUEST
            );
        }

        return venue;
    }

    private void applyRequest(Event event, EventRequest request) {
        event.setTitle(request.title().trim());
        event.setVenueId(request.venueId());
        event.setDate(request.date());
        event.setTime(request.time());
        event.setCategory(request.category().trim());
        event.setPrice(request.price());
        event.setCapacity(request.capacity());
        event.setDescription(request.description());
        event.setStatus(request.status());
    }
}