package org.afterglow.eventinventoryservice.service;

import org.afterglow.eventinventoryservice.dto.VenueRequest;
import org.afterglow.eventinventoryservice.model.Venue;

import java.util.List;

public interface VenueService {

    Venue createVenue(VenueRequest request);

    Venue updateVenue(String id, VenueRequest request);

    Venue getVenueById(String id);

    List<Venue> getAllVenues();
}