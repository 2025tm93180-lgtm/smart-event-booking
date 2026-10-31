package com.team.ticketnotification.ticket;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.ticketnotification.messaging.BookingConfirmedEvent;
import com.team.ticketnotification.messaging.InvalidBookingEventException;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class TicketIssuanceService {
    private static final Logger log=LoggerFactory.getLogger(TicketIssuanceService.class);
    private final JdbcTemplate db;
    private final Validator validator;
    private final ObjectMapper mapper;
    private final String deliveryMode;
    public TicketIssuanceService(JdbcTemplate db, Validator validator, ObjectMapper mapper,@Value("${app.notifications.mode:mock}") String mode) {
        this.db=db; this.validator=validator; this.mapper=mapper;
        this.deliveryMode=mode.toUpperCase(Locale.ROOT);
        if(!Set.of("MOCK","SMTP").contains(deliveryMode)) throw new IllegalArgumentException("Notification mode must be mock or smtp.");
    }
    @Transactional
    public boolean issue(BookingConfirmedEvent event) {
        if (event==null || !validator.validate(event).isEmpty())
            throw new InvalidBookingEventException("Confirmation has missing or invalid fields.");
        var seats=event.payload().items().stream().map(BookingConfirmedEvent.Item::seatId).sorted().toList();
        if (new HashSet<>(seats).size()!=seats.size())
            throw new InvalidBookingEventException("Seat IDs must be distinct.");
        String hash=fingerprint(event,seats);
        // The upsert serializes all confirmations for the same booking, including a new message ID.
        db.update("""
            INSERT INTO booking_issuances(booking_id,customer_id,event_id,contract_hash)
            VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE booking_id=booking_id
            """,event.bookingId(),event.payload().customerId(),event.payload().targetEventId(),hash);
        var issuance=db.queryForMap("SELECT contract_hash,issued_at FROM booking_issuances WHERE booking_id=? FOR UPDATE",event.bookingId());
        if (!hash.equals(issuance.get("contract_hash")))
            throw new InvalidBookingEventException("Conflicting confirmation for an existing booking.");
        var seen=db.queryForList("SELECT contract_hash FROM inbox_messages WHERE message_id=?",String.class,event.eventId());
        if (!seen.isEmpty()) {
            if (!hash.equals(seen.getFirst())) throw new InvalidBookingEventException("Message ID reused for a different booking payload.");
            log.info("Confirmation replay ignored bookingId={}",event.bookingId());
            return false;
        }
        boolean created=issuance.get("issued_at")==null;
        if (created) {
            Timestamp now=Timestamp.from(Instant.now());
            for (Long seat:seats) {
                String ticketId=UUID.randomUUID().toString();
                String code="TKT-"+UUID.randomUUID().toString().replace("-","");
                db.update("""
                    INSERT INTO tickets(id,booking_id,customer_id,event_id,seat_id,ticket_code,status,issued_at)
                    VALUES (?,?,?,?,?,?,'ISSUED',?)
                    """,ticketId,event.bookingId(),event.payload().customerId(),event.payload().targetEventId(),seat,code,now);
                db.update("""
                    INSERT INTO notification_attempts(id,booking_id,ticket_id,correlation_id,channel,status,idempotency_key,recipient_email,delivery_mode)
                    VALUES (?,?,?,?,'EMAIL','PENDING',?,?,?)
                    """,UUID.randomUUID().toString(),event.bookingId(),ticketId,event.correlationId(),"ticket:"+ticketId+":EMAIL:issued",event.payload().customerEmail(),deliveryMode);
            }
            db.update("UPDATE booking_issuances SET issued_at=? WHERE booking_id=?",now,event.bookingId());
        }
        try {
            db.update("INSERT INTO inbox_messages(message_id,message_type,contract_hash) VALUES (?,?,?)",event.eventId(),event.eventType(),hash);
        } catch (DuplicateKeyException ex) {
            // Same message ID raced across different booking locks. Roll back every write.
            throw new InvalidBookingEventException("Message ID already belongs to another confirmation.",ex);
        }
        log.info("Confirmation processed bookingId={} ticketsCreated={}",event.bookingId(),created ? seats.size():0);
        return created;
    }
    private String fingerprint(BookingConfirmedEvent event,List<Long> seats) {
        try {
            // Structured canonical data avoids delimiter collisions. Ordering does not change meaning.
            byte[] canonical=mapper.writeValueAsBytes(List.of(event.bookingId(),event.payload().customerId(),event.payload().targetEventId(),seats));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) { throw new IllegalStateException("Cannot fingerprint confirmation",ex); }
    }
}
