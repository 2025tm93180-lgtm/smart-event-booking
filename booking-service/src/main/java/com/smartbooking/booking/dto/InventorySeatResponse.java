package com.smartbooking.booking.dto;

import java.math.BigDecimal;

public record InventorySeatResponse(
        String id,
        String eventId,
        String seatId,
        BigDecimal price,
        String reservationId,
        String status
) {
}