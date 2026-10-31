package org.afterglow.eventinventoryservice.util;

import org.afterglow.eventinventoryservice.dto.EventRequest;
import org.afterglow.eventinventoryservice.dto.VenueRequest;
import org.afterglow.eventinventoryservice.exception.EventException;
import org.afterglow.eventinventoryservice.exception.VenueException;
import org.afterglow.eventinventoryservice.model.Venue;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ValidationUtil {

    private ValidationUtil() {
    }

    public static void validateVenue(VenueRequest request) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            throw new VenueException(
                "Venue request must not be null",
                HttpStatus.BAD_REQUEST
            );
        }

        if (isBlank(request.name())) {
            errors.add("Venue name is required");
        }

        if (isBlank(request.city())) {
            errors.add("City is required");
        }

        if (isBlank(request.address())) {
            errors.add("Address is required");
        }

        if (request.capacity() == null || request.capacity() <= 0) {
            errors.add("Venue capacity must be greater than zero");
        }

        throwIfVenueInvalid(errors);
    }

    public static void validateEvent(EventRequest request) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            throw new EventException(
                "Event request must not be null",
                HttpStatus.BAD_REQUEST
            );
        }

        if (isBlank(request.title())) {
            errors.add("Event title is required");
        }

        if (isBlank(request.venueId())) {
            errors.add("Venue ID is required");
        }

        if (request.date() == null) {
            errors.add("Event date is required");
        } else if (request.date().isBefore(LocalDate.now())) {
            errors.add("Event date cannot be in the past");
        }

        if (request.time() == null) {
            errors.add("Event start time is required");
        }

        if (isBlank(request.category())) {
            errors.add("Event category is required");
        }

        if (request.price() == null
                || request.price().signum() < 0) {
            errors.add("Event price must be zero or greater");
        }

        if (request.capacity() == null || request.capacity() <= 0) {
            errors.add("Ticket allocation must be greater than zero");
        }

        if (request.status() == null) {
            errors.add("Event status is required");
        }

        throwIfEventInvalid(errors);
    }

    public static void validateEventVenueCapacity(
            EventRequest request,
            Venue venue) {

        if (request.capacity() > venue.getCapacity()) {
            throw new EventException(
                "Ticket allocation (" + request.capacity()
                + ") cannot exceed venue capacity ("
                + venue.getCapacity() + ")",
                HttpStatus.BAD_REQUEST
            );
        }
    }

    public static void validateVenueCapacityForExistingEvents(
            VenueRequest request,
            List<Integer> existingEventCapacities) {

        if (request.capacity() == null) {
            return; // validateVenue handles this
        }

        int largestAllocation = existingEventCapacities.stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(0);

        if (request.capacity() < largestAllocation) {
            throw new VenueException(
                "Venue capacity cannot be lower than an existing "
                + "event's ticket allocation (" + largestAllocation + ")",
                HttpStatus.BAD_REQUEST
            );
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void throwIfVenueInvalid(List<String> errors) {
        if (!errors.isEmpty()) {
            throw new VenueException(
                String.join("; ", errors),
                HttpStatus.BAD_REQUEST
            );
        }
    }

    private static void throwIfEventInvalid(List<String> errors) {
        if (!errors.isEmpty()) {
            throw new EventException(
                String.join("; ", errors),
                HttpStatus.BAD_REQUEST
            );
        }
    }
}