
package com.smartbooking.booking.dto;

import java.util.List;

public record CreateReservationRequest(
        String bookingId,
        String eventId,
        List<String> seatIds
) {
}
