package com.team.ticketnotification.messaging;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.ticketnotification.ticket.TicketIssuanceService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component
public class BookingConfirmedListener {
    private final ObjectMapper mapper;
    private final TicketIssuanceService service;
    public BookingConfirmedListener(ObjectMapper mapper,TicketIssuanceService service) { this.mapper=mapper; this.service=service; }
    @KafkaListener(topics="${app.kafka.topic}")
    public void consume(ConsumerRecord<String,String> record) {
        try {
            BookingConfirmedEvent event=mapper.readValue(record.value(),BookingConfirmedEvent.class);
            if (event==null || event.bookingId()==null || !event.bookingId().equals(record.key()))
                throw new InvalidBookingEventException("Kafka key must equal bookingId.");
            if (event.correlationId()!=null && event.correlationId().matches("[A-Za-z0-9._:-]{1,100}"))
                MDC.put("correlationId",event.correlationId());
            service.issue(event); // A separate bean ensures the DB transaction commits before return.
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new InvalidBookingEventException("Invalid confirmation JSON.",ex);
        } finally { MDC.remove("correlationId"); }
    }
}
