package org.afterglow.eventinventoryservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.afterglow.eventinventoryservice.dto.VenueRequest;
import org.afterglow.eventinventoryservice.exception.VenueException;
import org.afterglow.eventinventoryservice.model.Venue;
import org.afterglow.eventinventoryservice.repo.EventRepository;
import org.afterglow.eventinventoryservice.repo.VenueRepository;
import org.afterglow.eventinventoryservice.service.VenueService;
import org.afterglow.eventinventoryservice.util.ValidationUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;

    public VenueServiceImpl(
            VenueRepository venueRepository,
            EventRepository eventRepository) {
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional
    public Venue createVenue(VenueRequest request) {
        log.info("Creating venue with name: {}", request.name());
        ValidationUtil.validateVenue(request);

        Venue venue = new Venue();
        applyRequest(venue, request);

        Venue savedVenue = venueRepository.save(venue);
        log.info("Created venue successfully with ID: {}", savedVenue.getId());
        return savedVenue;
    }

    @Override
    @Transactional
    public Venue updateVenue(String id, VenueRequest request) {
        log.info("Updating venue with ID: {}", id);
        ValidationUtil.validateVenue(request);

        Venue venue = venueRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Venue not found with ID: {}", id);
                return new VenueException(
                    "Venue not found with ID: " + id,
                    HttpStatus.NOT_FOUND
                );
            });

        List<Integer> allocations = eventRepository
            .findAll()
            .stream()
            .filter(event -> event.getVenueId().equals(id))
            .map(event -> event.getCapacity())
            .toList();

        ValidationUtil.validateVenueCapacityForExistingEvents(
            request, allocations
        );

        applyRequest(venue, request);
        Venue savedVenue = venueRepository.save(venue);
        log.info("Updated venue successfully with ID: {}", savedVenue.getId());
        return savedVenue;
    }

    @Override
    @Transactional(readOnly = true)
    public Venue getVenueById(String id) {
        log.info("Fetching venue with ID: {}", id);
        return venueRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Venue not found with ID: {}", id);
                return new VenueException(
                    "Venue not found with ID: " + id,
                    HttpStatus.NOT_FOUND
                );
            });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Venue> getAllVenues() {
        log.info("Fetching all venues");
        List<Venue> venues = venueRepository.findAll();
        log.info("Fetched {} venues", venues.size());
        return venues;
    }

    private void applyRequest(Venue venue, VenueRequest request) {
        venue.setName(request.name().trim());
        venue.setCity(request.city().trim());
        venue.setAddress(request.address().trim());
        venue.setCapacity(request.capacity());
        venue.setFacilities(request.facilities());

        if (request.active() != null) {
            venue.setActive(request.active());
        }
    }
}