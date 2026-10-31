package com.team.ticketnotification.ticket;
import com.team.ticketnotification.common.ApiException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class TicketQueries {
    public record Ticket(
            String id,
            String bookingId,
            String eventId,
            String seatId,
            String ticketCode,
            String status,
            Instant issuedAt
    ) {}
    public record Notification(String id,String bookingId,String ticketId,String channel,String status,int attemptCount,String lastError,String deliveryMode,boolean deliveryUncertain,Instant sentAt) {}
    public record Page<T>(List<T> items,int page,int size,long total) {}
    private final JdbcTemplate db;
    public TicketQueries(JdbcTemplate db) { this.db=db; }
    public Ticket ticket(String id,Jwt jwt) {
        var rows=db.query("SELECT * FROM tickets WHERE id=?",(r,n)->new Ticket(r.getString("id"),r.getString("booking_id"),r.getString("event_id"),
                r.getString("seat_id"),r.getString("ticket_code"),r.getString("status"),r.getTimestamp("issued_at").toInstant()),id);
        if(rows.isEmpty()) throw ApiException.missing();
        authorize(rows.getFirst().bookingId(),jwt);
        return rows.getFirst();
    }
    public Page<Ticket> tickets(String bookingId,int page,int size,Jwt jwt) {
        authorize(bookingId,jwt); validatePage(page,size);
        var rows=db.query("SELECT * FROM tickets WHERE booking_id=? ORDER BY seat_id LIMIT ? OFFSET ?",(r,n)->new Ticket(r.getString("id"),r.getString("booking_id"),r.getString("event_id"),
                r.getString("seat_id"),r.getString("ticket_code"),r.getString("status"),r.getTimestamp("issued_at").toInstant()),bookingId,size,(long)page*size);
        return new Page<>(rows,page,size,db.queryForObject("SELECT COUNT(*) FROM tickets WHERE booking_id=?",Long.class,bookingId));
    }
    public Page<Notification> notifications(String bookingId,int page,int size,Jwt jwt) {
        authorize(bookingId,jwt); validatePage(page,size);
        var rows=db.query("SELECT * FROM notification_attempts WHERE booking_id=? ORDER BY created_at,id LIMIT ? OFFSET ?",(r,n)->new Notification(r.getString("id"),r.getString("booking_id"),r.getString("ticket_id"),r.getString("channel"),r.getString("status"),r.getInt("attempt_count"),r.getString("last_error"),r.getString("delivery_mode"),r.getBoolean("delivery_uncertain"),r.getTimestamp("sent_at")==null ? null:r.getTimestamp("sent_at").toInstant()),bookingId,size,(long)page*size);
        return new Page<>(rows,page,size,db.queryForObject("SELECT COUNT(*) FROM notification_attempts WHERE booking_id=?",Long.class,bookingId));
    }
    @Transactional
    public void retry(String id,boolean allowDuplicate) {
        var states=db.queryForList("SELECT status,delivery_uncertain FROM notification_attempts WHERE id=? FOR UPDATE",id);
        if(states.isEmpty()) throw ApiException.missing();
        if(!"FAILED".equals(states.getFirst().get("status"))) throw ApiException.conflict("Only a failed notification can be retried.");
        if(Boolean.TRUE.equals(states.getFirst().get("delivery_uncertain")) && !allowDuplicate)
            throw ApiException.conflict("Previous delivery may have succeeded. Check provider logs, then retry with allowDuplicate=true if needed.");
        db.update("UPDATE notification_attempts SET delivery_uncertain=FALSE,status='PENDING',last_error=NULL,updated_at=CURRENT_TIMESTAMP(6) WHERE id=?",id);
    }
    private void authorize(String bookingId,Jwt jwt) {
        var owners=db.queryForList("SELECT customer_id FROM booking_issuances WHERE booking_id=? AND issued_at IS NOT NULL",String.class,bookingId);
        if(owners.isEmpty()) throw ApiException.missing();
        boolean admin="ADMIN".equals(jwt.getClaimAsString("role"));
        if(!owners.getFirst().equals(jwt.getSubject()) && !admin)
            throw new ApiException(HttpStatus.FORBIDDEN,"FORBIDDEN","You do not own this booking.");
    }
    private void validatePage(int page,int size) {
        if(page<0 || page>100000 || size<1 || size>100) throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_PAGE","page must be 0..100000 and size 1..100.");
    }
}
