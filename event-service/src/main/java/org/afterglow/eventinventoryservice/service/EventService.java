package org.afterglow.eventinventoryservice.service;

import org.afterglow.eventinventoryservice.dto.EventRequest;
import org.afterglow.eventinventoryservice.model.Event;

import java.util.List;

public interface EventService {

    Event createEvent(EventRequest request);

    Event updateEvent(String id, EventRequest request);

    List<Event> getAllEvents();

    Event getEventById(String id);

    void deleteEvent(String id);
}