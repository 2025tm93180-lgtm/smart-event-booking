package org.afterglow.eventinventoryservice;

import org.afterglow.eventinventoryservice.dto.EventRequest;
import org.afterglow.eventinventoryservice.dto.VenueRequest;
import org.afterglow.eventinventoryservice.exception.ErrorResponse;
import org.afterglow.eventinventoryservice.exception.EventException;
import org.afterglow.eventinventoryservice.exception.GlobalExceptionHandler;
import org.afterglow.eventinventoryservice.exception.ResourceNotFoundException;
import org.afterglow.eventinventoryservice.exception.VenueException;
import org.afterglow.eventinventoryservice.model.Event;
import org.afterglow.eventinventoryservice.model.EventStatus;
import org.afterglow.eventinventoryservice.model.Venue;
import org.afterglow.eventinventoryservice.service.EventService;
import org.afterglow.eventinventoryservice.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EventInventoryServiceApplicationTests {

    @Autowired
    private VenueService venueService;

    @Autowired
    private EventService eventService;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    void contextLoads() {
    }

    @Test
    void testVenueAndEventIdFormat() {
        Venue venue = venueService.createVenue(new VenueRequest(
                "Grand Arena",
                "New York",
                "123 Broadway",
                500,
                "WiFi, Parking",
                true
        ));

        assertNotNull(venue.getId());
        assertTrue(venue.getId().matches("^VEN-\\d{5}$"), "Venue ID should match VEN-XXXXX format but was: " + venue.getId());

        Event event = eventService.createEvent(new EventRequest(
                "Rock Concert",
                venue.getId(),
                LocalDate.now().plusDays(10),
                LocalTime.of(19, 0),
                "Music",
                BigDecimal.valueOf(99.99),
                200,
                "Live rock music event",
                EventStatus.PUBLISHED
        ));

        assertNotNull(event.getId());
        assertTrue(event.getId().matches("^EVE-\\d{5}$"), "Event ID should match EVE-XXXXX format but was: " + event.getId());
    }

    @Test
    void testCannotCreateMultipleEventsOnSameDayAtSameVenue() {
        Venue venue = venueService.createVenue(new VenueRequest(
                "City Hall",
                "Chicago",
                "100 Main St",
                300,
                "Audio, Lighting",
                true
        ));

        LocalDate eventDate = LocalDate.now().plusDays(15);

        Event event1 = eventService.createEvent(new EventRequest(
                "Jazz Night",
                venue.getId(),
                eventDate,
                LocalTime.of(18, 0),
                "Music",
                BigDecimal.valueOf(50.00),
                100,
                "Jazz Performance",
                EventStatus.PUBLISHED
        ));
        assertNotNull(event1.getId());

        // Attempt to create second event on same day at same venue
        EventRequest conflictingRequest = new EventRequest(
                "Comedy Night",
                venue.getId(),
                eventDate,
                LocalTime.of(21, 0),
                "Comedy",
                BigDecimal.valueOf(40.00),
                150,
                "Stand-up Comedy",
                EventStatus.PUBLISHED
        );

        EventException ex = assertThrows(
                EventException.class,
                () -> eventService.createEvent(conflictingRequest)
        );
        assertTrue(ex.getMessage().contains("already scheduled"));

        // Creating on different date at same venue should succeed
        Event eventOnDifferentDay = eventService.createEvent(new EventRequest(
                "Art Exhibition",
                venue.getId(),
                eventDate.plusDays(1),
                LocalTime.of(10, 0),
                "Art",
                BigDecimal.valueOf(20.00),
                50,
                "Art show",
                EventStatus.PUBLISHED
        ));
        assertNotNull(eventOnDifferentDay.getId());

        // Updating event1 without changing date/venue should succeed
        Event updatedEvent1 = eventService.updateEvent(event1.getId(), new EventRequest(
                "Jazz Night Extended",
                venue.getId(),
                eventDate,
                LocalTime.of(18, 0),
                "Music",
                BigDecimal.valueOf(55.00),
                120,
                "Jazz Performance with bonus set",
                EventStatus.PUBLISHED
        ));
        assertEquals("Jazz Night Extended", updatedEvent1.getTitle());

        // Updating eventOnDifferentDay to the conflict date should fail
        EventRequest conflictingUpdateRequest = new EventRequest(
                "Art Exhibition",
                venue.getId(),
                eventDate,
                LocalTime.of(10, 0),
                "Art",
                BigDecimal.valueOf(20.00),
                50,
                "Art show",
                EventStatus.PUBLISHED
        );
        assertThrows(
                EventException.class,
                () -> eventService.updateEvent(eventOnDifferentDay.getId(), conflictingUpdateRequest)
        );
    }

    @Test
    void testCustomVenueExceptionHandling() {
        VenueException ex = assertThrows(
                VenueException.class,
                () -> venueService.getVenueById("NON_EXISTENT_ID")
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleVenueException(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VenueException", response.getBody().getCustomException());
        assertEquals(404, response.getBody().getHttpStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, response.getBody().getHttpStatus());
        assertTrue(response.getBody().getMessage().contains("Venue not found with ID: NON_EXISTENT_ID"));
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testCustomEventExceptionHandling() {
        EventException ex = assertThrows(
                EventException.class,
                () -> eventService.getEventById("NON_EXISTENT_EVENT_ID")
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleEventException(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("EventException", response.getBody().getCustomException());
        assertEquals(404, response.getBody().getHttpStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, response.getBody().getHttpStatus());
        assertTrue(response.getBody().getMessage().contains("Event not found with ID: NON_EXISTENT_EVENT_ID"));
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testGlobalExceptionHandlerGeneralAndResourceNotFound() {
        ResourceNotFoundException notFoundEx = new ResourceNotFoundException("Item not found");
        ResponseEntity<ErrorResponse> notFoundResponse = globalExceptionHandler.handleNotFound(notFoundEx);
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatusCode());
        assertEquals("ResourceNotFoundException", notFoundResponse.getBody().getCustomException());
        assertEquals(404, notFoundResponse.getBody().getHttpStatusCode());
        assertEquals("Item not found", notFoundResponse.getBody().getMessage());

        IllegalArgumentException badRequestEx = new IllegalArgumentException("Bad input parameter");
        ResponseEntity<ErrorResponse> badRequestResponse = globalExceptionHandler.handleBadRequest(badRequestEx);
        assertEquals(HttpStatus.BAD_REQUEST, badRequestResponse.getStatusCode());
        assertEquals("IllegalArgumentException", badRequestResponse.getBody().getCustomException());
        assertEquals(400, badRequestResponse.getBody().getHttpStatusCode());
        assertEquals("Bad input parameter", badRequestResponse.getBody().getMessage());

        Exception generalEx = new RuntimeException("Server error occurred");
        ResponseEntity<ErrorResponse> generalResponse = globalExceptionHandler.handleGeneralException(generalEx);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, generalResponse.getStatusCode());
        assertEquals("RuntimeException", generalResponse.getBody().getCustomException());
        assertEquals(500, generalResponse.getBody().getHttpStatusCode());
        assertEquals("Server error occurred", generalResponse.getBody().getMessage());
    }

    @Test
    void testConcurrentEventCreationOnSameDayAtSameVenue() throws InterruptedException {
        Venue venue = venueService.createVenue(new VenueRequest(
                "Olympic Stadium",
                "Los Angeles",
                "1000 Stadium Way",
                5000,
                "VIP Lounges, Big Screen",
                true
        ));

        LocalDate eventDate = LocalDate.now().plusDays(30);
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    eventService.createEvent(new EventRequest(
                            "Concurrent Championship " + index,
                            venue.getId(),
                            eventDate,
                            LocalTime.of(15, 0),
                            "Sports",
                            BigDecimal.valueOf(100.00),
                            500,
                            "Major Tournament",
                            EventStatus.PUBLISHED
                    ));
                    successCount.incrementAndGet();
                } catch (EventException | org.springframework.dao.DataIntegrityViolationException ex) {
                    failureCount.incrementAndGet();
                } catch (Exception ex) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executorService.shutdown();

        assertEquals(1, successCount.get(), "Exactly one concurrent request must succeed in creating an event on the same day & venue");
        assertEquals(numberOfThreads - 1, failureCount.get(), "All other concurrent requests must fail");
    }
}
