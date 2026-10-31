package com.team.ticketnotification.notification;
public class EmailDeliveryException extends RuntimeException {
    private final boolean uncertain;
    public EmailDeliveryException(String safeMessage,boolean uncertain) {super(safeMessage);this.uncertain=uncertain;}
    public boolean uncertain() {return uncertain;}
}
