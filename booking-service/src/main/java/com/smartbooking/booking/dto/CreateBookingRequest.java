
package com.smartbooking.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateBookingRequest(
        @NotBlank
        String eventId,

        @NotEmpty
        @Size(min = 1, max = 10)
        List<@NotBlank String> seatIds
) {
}
