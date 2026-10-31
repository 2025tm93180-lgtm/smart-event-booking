package com.team.ticketnotification.notification;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
@Service
public class NotificationDeliveryStore {
    public record Delivery(String id,String token,String mode,String correlationId,String recipient,
        String bookingId,String eventId,String seatId,String ticketCode,Instant issuedAt) {}
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    public NotificationDeliveryStore(JdbcTemplate db,TransactionTemplate tx) {this.db=db;this.tx=tx;}
    public Delivery claim() {
        return tx.execute(status->{
            var rows=db.queryForList("SELECT id FROM notification_attempts WHERE status='PENDING' AND claim_token IS NULL ORDER BY created_at,id LIMIT 1 FOR UPDATE SKIP LOCKED");
            if(rows.isEmpty()) return null;
            String id=(String)rows.getFirst().get("id");
            String token=UUID.randomUUID().toString();
            db.update("UPDATE notification_attempts SET claim_token=?,lease_until=?,attempt_count=attempt_count+1,updated_at=CURRENT_TIMESTAMP(6) WHERE id=?",
                token,Timestamp.from(Instant.now().plusSeconds(120)),id);
            return db.queryForObject("""
                SELECT n.id,n.delivery_mode,n.correlation_id,n.recipient_email,t.booking_id,t.event_id,t.seat_id,t.ticket_code,t.issued_at
                FROM notification_attempts n JOIN tickets t ON t.id=n.ticket_id WHERE n.id=?
                """,(rs,i)->new Delivery(id,token,rs.getString("delivery_mode"),rs.getString("correlation_id"),rs.getString("recipient_email"),
                    rs.getString("booking_id"),rs.getString("event_id"),rs.getString("seat_id"),rs.getString("ticket_code"),rs.getTimestamp("issued_at").toInstant()),id);
        });
    }
    public void recoverExpiredClaims() {
        // Do not automatically resend a message whose previous process may already have sent it.
        db.update("""
            UPDATE notification_attempts SET status='FAILED',delivery_uncertain=TRUE,
                last_error='Delivery interrupted; check the SMTP provider before retrying.',claim_token=NULL,lease_until=NULL,updated_at=CURRENT_TIMESTAMP(6)
            WHERE status='PENDING' AND claim_token IS NOT NULL AND lease_until < ?
            """,Timestamp.from(Instant.now()));
    }
    public boolean finish(Delivery d,boolean success,boolean uncertain,String error) {
        return db.update("""
            UPDATE notification_attempts SET status=?,delivery_uncertain=?,last_error=?,sent_at=?,
                claim_token=NULL,lease_until=NULL,updated_at=CURRENT_TIMESTAMP(6)
            WHERE id=? AND claim_token=? AND status='PENDING'
            """,success?"SENT":"FAILED",uncertain,error,success?Timestamp.from(Instant.now()):null,d.id(),d.token())==1;
    }
}
