package com.team.ticketnotification;
import com.team.ticketnotification.messaging.BookingConfirmedEvent;
import com.team.ticketnotification.notification.*;
import com.team.ticketnotification.ticket.*;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties={"app.notifications.mode=smtp","app.notifications.from=tickets@example.test",
    "spring.datasource.url=jdbc:h2:mem:smtp;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"})
@ActiveProfiles("test")
class SmtpDeliveryTest {
    static final CaptureSmtpServer smtp=new CaptureSmtpServer();
    @DynamicPropertySource static void mail(DynamicPropertyRegistry r) {
        r.add("spring.mail.host",()->"127.0.0.1");r.add("spring.mail.port",()->smtp.port());
        r.add("spring.mail.properties[mail.smtp.auth]",()->"false");
        r.add("spring.mail.properties[mail.smtp.starttls.enable]",()->"false");
        r.add("spring.mail.properties[mail.smtp.starttls.required]",()->"false");
        r.add("spring.mail.properties[mail.smtp.ssl.enable]",()->"false");
        r.add("spring.mail.properties[mail.smtp.timeout]",()->1000);
    }
    @Autowired TicketIssuanceService issuance;
    @Autowired JdbcTemplate db;
    @Autowired NotificationDeliveryStore store;
    @Autowired SmtpEmailSender sender;
    @Autowired TicketQueries queries;
    @BeforeEach void reset() {
        db.update("DELETE FROM notification_attempts");db.update("DELETE FROM tickets");db.update("DELETE FROM inbox_messages");db.update("DELETE FROM booking_issuances");
        smtp.messages.clear();smtp.dropAck=false;
    }
    @AfterAll static void shutdown() throws IOException {smtp.close();}
    BookingConfirmedEvent event(String message,String email,Long... seats) {
        return new BookingConfirmedEvent(message,1,"booking.confirmed.v1","smtp-test",Instant.now(),"email-booking",
            new BookingConfirmedEvent.Payload("customer-001",101L,Instant.now(),Arrays.stream(seats).map(BookingConfirmedEvent.Item::new).toList(),email));
    }
    NotificationWorker worker() {return new NotificationWorker(store,Optional.of(sender),false,20);}
    @Test void actualSmtpSubmissionPersistsSentAndDuplicateBookingDoesNotResend() throws Exception {
        var event=event("smtp-1","recipient@example.test",501L,502L);
        issuance.issue(event);
        assertThat(db.queryForList("SELECT recipient_email FROM notification_attempts",String.class)).containsOnly("recipient@example.test");
        worker().deliverPending();
        assertThat(smtp.messages).hasSize(2);
        for(String raw:smtp.messages) {
            var message=new MimeMessage(Session.getInstance(new Properties()),new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
            assertThat(message.getAllRecipients()[0].toString()).isEqualTo("recipient@example.test");
            assertThat(message.getFrom()[0].toString()).isEqualTo("tickets@example.test");
            assertThat(message.getSubject()).isEqualTo("Your event ticket is ready");
            assertThat(message.getHeader("X-Notification-ID")).isNotEmpty();
            assertThat(message.getMessageID()).contains("ticket-notification-");
            assertThat(raw).contains("email-booking","TKT-");
        }
        assertThat(db.queryForList("SELECT status FROM notification_attempts",String.class)).containsOnly("SENT");
        assertThat(db.queryForObject("SELECT COUNT(*) FROM notification_attempts WHERE sent_at IS NOT NULL",Long.class)).isEqualTo(2);
        issuance.issue(event);issuance.issue(event("smtp-2","recipient@example.test",501L,502L));worker().deliverPending();
        assertThat(smtp.messages).hasSize(2);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
    }
    @Test void missingEmailFailsNotificationWithoutLosingTicket() {
        issuance.issue(event("missing",null,501L));worker().deliverPending();
        assertThat(smtp.messages).isEmpty();
        assertThat(db.queryForObject("SELECT status FROM notification_attempts",String.class)).isEqualTo("FAILED");
        assertThat(db.queryForObject("SELECT delivery_uncertain FROM notification_attempts",Boolean.class)).isFalse();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM tickets",Long.class)).isEqualTo(1);
    }
    @Test void lostSmtpAcknowledgementRequiresExplicitDuplicateAcknowledgement() {
        smtp.dropAck=true;
        issuance.issue(event("ambiguous","recipient@example.test",501L));worker().deliverPending();
        assertThat(smtp.messages).hasSize(1);
        assertThat(db.queryForObject("SELECT status FROM notification_attempts",String.class)).isEqualTo("FAILED");
        assertThat(db.queryForObject("SELECT delivery_uncertain FROM notification_attempts",Boolean.class)).isTrue();
        String id=db.queryForObject("SELECT id FROM notification_attempts",String.class);
        assertThatThrownBy(()->queries.retry(id,false)).hasMessageContaining("may have succeeded");
        worker().deliverPending();assertThat(smtp.messages).hasSize(1);
        smtp.dropAck=false;queries.retry(id,true);worker().deliverPending();
        assertThat(smtp.messages).hasSize(2); // An intentional admin resend after checking provider logs.
        assertThat(db.queryForObject("SELECT attempt_count FROM notification_attempts",Integer.class)).isEqualTo(2);
        assertThat(db.queryForObject("SELECT status FROM notification_attempts",String.class)).isEqualTo("SENT");
    }
    @Test void staleClaimIsNotAutomaticallyResent() {
        issuance.issue(event("stale","recipient@example.test",501L));var claimed=store.claim();
        db.update("UPDATE notification_attempts SET lease_until=? WHERE id=?",Timestamp.from(Instant.now().minusSeconds(1)),claimed.id());
        worker().deliverPending();assertThat(smtp.messages).isEmpty();
        assertThat(db.queryForObject("SELECT delivery_uncertain FROM notification_attempts",Boolean.class)).isTrue();
        assertThat(store.finish(claimed,true,false,null)).isFalse(); // A stale worker cannot overwrite recovered state.
        assertThatThrownBy(()->queries.retry(claimed.id(),false)).hasMessageContaining("may have succeeded");
    }
    @Test void claimIsExclusiveAcrossConcurrentWorkers() throws Exception {
        issuance.issue(event("concurrent","recipient@example.test",501L));
        try(var pool=Executors.newFixedThreadPool(4)) {
            var tasks=List.<Callable<NotificationDeliveryStore.Delivery>>of(store::claim,store::claim,store::claim,store::claim);
            int claimed=0;for(var result:pool.invokeAll(tasks))if(result.get()!=null)claimed++;
            assertThat(claimed).isEqualTo(1);
        }
    }
    @Test void emailHeaderInjectionIsRejectedBeforePersistence() {
        assertThatThrownBy(()->issuance.issue(event("bad","victim@example.test\r\nBcc:other@example.test",501L))).isInstanceOf(com.team.ticketnotification.messaging.InvalidBookingEventException.class);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM tickets",Long.class)).isZero();
    }
    // Minimal local-only SMTP receiver. Captures bytes; never relays any email externally.
    static class CaptureSmtpServer implements AutoCloseable {
        final ServerSocket socket;
        final List<String> messages=new CopyOnWriteArrayList<>();
        final ExecutorService executor=Executors.newSingleThreadExecutor();
        volatile boolean dropAck;
        CaptureSmtpServer() {
            try {socket=new ServerSocket(0,20,InetAddress.getLoopbackAddress());}
            catch(IOException ex) {throw new UncheckedIOException(ex);}
            executor.submit(()->{while(!socket.isClosed())try(Socket client=socket.accept()){receive(client);}catch(IOException ex){if(!socket.isClosed())throw new UncheckedIOException(ex);}});
        }
        int port() {return socket.getLocalPort();}
        void receive(Socket client) throws IOException {
            client.setSoTimeout(5000);
            var in=new BufferedReader(new InputStreamReader(client.getInputStream(),StandardCharsets.UTF_8));
            var out=new PrintWriter(new OutputStreamWriter(client.getOutputStream(),StandardCharsets.UTF_8),true);
            out.print("220 localhost test SMTP\r\n");out.flush();
            for(String line;(line=in.readLine())!=null;) {
                if(line.startsWith("EHLO") || line.startsWith("HELO"))out.print("250 localhost\r\n");
                else if(line.equals("DATA")) {
                    out.print("354 End with dot\r\n");out.flush();var body=new StringBuilder();
                    for(String part;(part=in.readLine())!=null && !part.equals(".");)body.append(part.startsWith("..")?part.substring(1):part).append("\r\n");
                    messages.add(body.toString());if(dropAck)return;out.print("250 queued\r\n");
                } else if(line.equals("QUIT")){out.print("221 bye\r\n");out.flush();return;}
                else out.print("250 OK\r\n");
                out.flush();
            }
        }
        public void close() throws IOException {socket.close();executor.shutdownNow();}
    }
}
