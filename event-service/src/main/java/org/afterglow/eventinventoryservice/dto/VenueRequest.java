package org.afterglow.eventinventoryservice.dto;

public record VenueRequest(
    String name,
    String city,
    String address,
    Integer capacity,
    String facilities,
    Boolean active
) {}