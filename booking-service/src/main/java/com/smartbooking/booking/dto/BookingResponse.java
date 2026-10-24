
package com.smartbooking.booking.dto;

import java.math.BigDecimal;
import java.util.List;

public record BookingResponse(
        String bookingId,
        String eventId,
        List<String> seatIds,
        BigDecimal totalAmount,
        String currency,
        String status
) {
}
