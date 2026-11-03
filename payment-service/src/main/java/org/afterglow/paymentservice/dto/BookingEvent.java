package org.afterglow.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingEvent {

    @JsonProperty("bookingId")
    @JsonAlias({"booking_id", "id", "aggregateId"})
    private String bookingId;

    @JsonProperty("eventId")
    @JsonAlias({"event_id"})
    private String eventId;

    @JsonProperty("seatIds")
    @JsonAlias({"seat_ids", "seats"})
    private List<String> seatIds;

    @JsonProperty("totalAmount")
    @JsonAlias({"amount", "total_amount", "price"})
    private Double totalAmount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("eventType")
    @JsonAlias({"event_type", "type", "status"})
    private String eventType;

    @JsonProperty("messageId")
    @JsonAlias({"message_id", "eventId", "event_id"})
    private String messageId;

    public Double getEffectiveAmount() {
        if (totalAmount != null) {
            return totalAmount;
        }
        return 0.0;
    }
}
