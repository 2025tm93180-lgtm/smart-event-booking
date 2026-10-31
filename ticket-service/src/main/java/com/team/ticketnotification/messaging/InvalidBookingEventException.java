package com.team.ticketnotification.messaging;
public class InvalidBookingEventException extends RuntimeException {
    public InvalidBookingEventException(String message) { super(message); }
    public InvalidBookingEventException(String message, Throwable cause) { super(message,cause); }
}
