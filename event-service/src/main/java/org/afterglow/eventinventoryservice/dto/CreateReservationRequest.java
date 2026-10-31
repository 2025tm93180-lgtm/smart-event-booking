package org.afterglow.eventinventoryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateReservationRequest(@NotBlank String bookingId, @NotBlank String eventId, @NotEmpty List<String> seatIds) {}
