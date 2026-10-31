
package com.smartbooking.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryReservationResponse(
        String id,
        String bookingId,
        String eventId,
        String seatIds,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant expiresAt,
        String status
) {
}
