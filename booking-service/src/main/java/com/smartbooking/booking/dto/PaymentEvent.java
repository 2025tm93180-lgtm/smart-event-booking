package com.smartbooking.booking.dto;

public record PaymentEvent(
        String paymentId,
        String bookingId,
        String status,
        String failureReason
) {
}