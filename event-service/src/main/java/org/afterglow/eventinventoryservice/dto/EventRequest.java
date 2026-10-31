package org.afterglow.eventinventoryservice.dto;

import org.afterglow.eventinventoryservice.model.EventStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record EventRequest(
    String title,
    String venueId,
    LocalDate date,
    LocalTime time,
    String category,
    BigDecimal price,
    Integer capacity,
    String description,
    EventStatus status
) {}