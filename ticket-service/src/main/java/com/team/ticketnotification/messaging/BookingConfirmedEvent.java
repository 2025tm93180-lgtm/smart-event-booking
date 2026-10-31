package com.team.ticketnotification.messaging;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
public record BookingConfirmedEvent(
    @NotBlank @Size(max=100) String eventId,
    @NotNull @Min(1) @Max(1) Integer schemaVersion,
    @NotBlank @Pattern(regexp="booking\\.confirmed\\.v1") String eventType,
    @NotBlank @Pattern(regexp="[A-Za-z0-9._:-]{1,100}") String correlationId,
    @NotNull Instant occurredAt,
    @NotBlank @Size(max=100) String bookingId,
    @NotNull @Valid Payload payload) {
    public record Payload(
        @NotBlank @Size(max=100) String customerId,
        @NotNull @Positive Long targetEventId,
        @NotNull Instant confirmedAt,
        @NotEmpty @Size(max=100) List<@NotNull @Valid Item> items,
        @Email @Size(max=254) @Pattern(regexp="[^\\r\\n]*") String customerEmail) {
        // Source compatibility for existing tests/producers without an email address.
        public Payload(String customerId,Long targetEventId,Instant confirmedAt,List<Item> items) {
            this(customerId,targetEventId,confirmedAt,items,null);
        }
    }
    public record Item(@NotNull @Positive Long seatId) {}
}
