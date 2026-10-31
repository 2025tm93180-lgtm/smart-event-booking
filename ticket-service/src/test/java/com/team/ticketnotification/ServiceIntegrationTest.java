package com.team.ticketnotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.ticketnotification.messaging.*;
import com.team.ticketnotification.ticket.*;
import com.team.ticketnotification.notification.NotificationWorker;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceIntegrationTest {
    @Autowired TicketIssuanceService service;
    @Autowired JdbcTemplate db;
    @Autowired MockMvc mvc;
    @Autowired TransactionTemplate transactions;
    @Autowired com.team.ticketnotification.notification.NotificationDeliveryStore deliveryStore;
    @Autowired ObjectMapper mapper;
    @Autowired BookingConfirmedListener listener;
    @BeforeEach void clean() {
        db.update("DELETE FROM notification_attempts"); db.update("DELETE FROM tickets");
        db.update("DELETE FROM inbox_messages"); db.update("DELETE FROM booking_issuances");
    }
    BookingConfirmedEvent event(String message,String booking,String customer,Long... seats) {
        return new BookingConfirmedEvent(message,1,"booking.confirmed.v1","corr-test",Instant.now(),booking,
            new BookingConfirmedEvent.Payload(customer,101L,Instant.now(),Arrays.stream(seats).map(BookingConfirmedEvent.Item::new).toList()));
    }
    long count(String table) { return db.queryForObject("SELECT COUNT(*) FROM "+table,Long.class); }
    @Test void createsOneTicketAndPendingNotificationPerSeat() {
        assertThat(service.issue(event("m1","b1","c1",501L,502L))).isTrue();
        assertThat(count("tickets")).isEqualTo(2);assertThat(count("notification_attempts")).isEqualTo(2);assertThat(count("inbox_messages")).isEqualTo(1);
        assertThat(db.queryForList("SELECT status FROM notification_attempts",String.class)).containsOnly("PENDING");
    }
    @Test void sameMessageAndNewMessageReplayPreserveTicketIds() {
        service.issue(event("m1","b1","c1",501L,502L));
        var ids=db.queryForList("SELECT id FROM tickets ORDER BY id",String.class);
        assertThat(service.issue(event("m1","b1","c1",502L,501L))).isFalse();
        assertThat(service.issue(event("m2","b1","c1",501L,502L))).isFalse();
        assertThat(db.queryForList("SELECT id FROM tickets ORDER BY id",String.class)).isEqualTo(ids);
        assertThat(count("notification_attempts")).isEqualTo(2);assertThat(count("inbox_messages")).isEqualTo(2);
    }
    @Test void conflictingBookingAndReusedMessageAreRejectedAtomically() {
        service.issue(event("m1","b1","c1",501L));
        assertThatThrownBy(()->service.issue(event("m2","b1","c1",501L,502L))).isInstanceOf(InvalidBookingEventException.class);
        assertThatThrownBy(()->service.issue(event("m1","b2","c2",503L))).isInstanceOf(InvalidBookingEventException.class);
        assertThat(count("tickets")).isEqualTo(1);assertThat(count("booking_issuances")).isEqualTo(1);
    }
    @Test void duplicateSeatAndInvalidVersionHaveNoEffects() {
        assertThatThrownBy(()->service.issue(event("m1","b1","c1",501L,501L))).isInstanceOf(InvalidBookingEventException.class);
        var good=event("m2","b2","c2",501L);
        var bad=new BookingConfirmedEvent(good.eventId(),2,good.eventType(),good.correlationId(),good.occurredAt(),good.bookingId(),good.payload());
        assertThatThrownBy(()->service.issue(bad)).isInstanceOf(InvalidBookingEventException.class);
        assertThat(count("tickets")).isZero();assertThat(count("inbox_messages")).isZero();
    }
    @Test void rollbackThenRetryWorks() {
        var event=event("m1","b1","c1",501L,502L);
        assertThatThrownBy(()->transactions.executeWithoutResult(s->{service.issue(event);throw new IllegalStateException("forced crash before commit");})).isInstanceOf(IllegalStateException.class);
        assertThat(count("tickets")).isZero();assertThat(count("notification_attempts")).isZero();assertThat(count("inbox_messages")).isZero();
        service.issue(event);assertThat(count("tickets")).isEqualTo(2);
    }
    @Test void twentyConcurrentLogicalDuplicatesCreateOnlyTwoTickets() throws Exception {
        try(var pool=Executors.newFixedThreadPool(8)) {
            var calls=new ArrayList<Callable<Boolean>>();
            for(int i=0;i<20;i++) { String message="m"+i;calls.add(()->service.issue(event(message,"b1","c1",501L,502L))); }
            int created=0;
            for(var future:pool.invokeAll(calls)) if(future.get(20,TimeUnit.SECONDS)) created++;
            assertThat(created).isEqualTo(1);
        }
        assertThat(count("tickets")).isEqualTo(2);assertThat(count("notification_attempts")).isEqualTo(2);assertThat(count("inbox_messages")).isEqualTo(20);
    }
    @Test void mockFailureRetryAndSuccessPreserveTickets() {
        service.issue(event("m1","b1","c1",501L));
        var failWorker=new NotificationWorker(deliveryStore,Optional.empty(),true,20);
        failWorker.deliverPending();
        assertThat(db.queryForObject("SELECT status FROM notification_attempts",String.class)).isEqualTo("FAILED");
        assertThat(count("tickets")).isEqualTo(1);
        String id=db.queryForObject("SELECT id FROM notification_attempts",String.class);
        new TicketQueries(db).retry(id,false);
        var successWorker=new NotificationWorker(deliveryStore,Optional.empty(),false,20);
        successWorker.deliverPending();
        assertThat(db.queryForObject("SELECT status FROM notification_attempts",String.class)).isEqualTo("SENT");
        assertThat(db.queryForObject("SELECT attempt_count FROM notification_attempts",Integer.class)).isEqualTo(2);
        assertThat(count("notification_attempts")).isEqualTo(1);
    }
    @Test void httpOwnershipAuthenticationPaginationAndAdminRetry() throws Exception {
        service.issue(event("m1","b1","c1",501L,502L));
        mvc.perform(get("/api/v1/tickets/by-booking/b1")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/v1/tickets/by-booking/b1").with(jwt().jwt(j->j.subject("c1")))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2));
        mvc.perform(get("/api/v1/tickets/by-booking/b1").with(jwt().jwt(j->j.subject("c2")))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/notifications/by-booking/b1").with(jwt().jwt(j->j.subject("c2")))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/tickets/by-booking/b1?size=101").with(jwt().jwt(j->j.subject("c1")))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/tickets/by-booking/b1?page=no").with(jwt().jwt(j->j.subject("c1")))).andExpect(status().isBadRequest());
        String id=db.queryForObject("SELECT id FROM notification_attempts LIMIT 1",String.class);
        mvc.perform(post("/api/v1/notifications/retry/"+id).with(jwt().jwt(j->j.subject("c1")))).andExpect(status().isForbidden());
        db.update("UPDATE notification_attempts SET status='FAILED' WHERE id=?",id);
        mvc.perform(post("/api/v1/notifications/retry/"+id).with(jwt().jwt(j->j.subject("admin").claim("role","ADMIN")).authorities(()->"ROLE_ADMIN"))).andExpect(status().isAccepted());
        mvc.perform(post("/api/v1/notifications/retry/"+id).with(jwt().authorities(()->"ROLE_ADMIN"))).andExpect(status().isConflict());
    }
    String signedToken(String issuer,String audience,Instant expiry) {
        byte[] secret="test-only-secret-with-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8);
        var encoder=new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(secret,"HmacSHA256")));
        var claims=JwtClaimsSet.builder().subject("c1").issuer(issuer).audience(List.of(audience)).issuedAt(Instant.now().minusSeconds(300)).expiresAt(expiry).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
    }
    @Test void realJwtDecoderChecksSignatureIssuerAudienceAndExpiry() throws Exception {
        service.issue(event("m1","b1","c1",501L));
        var valid=signedToken("smart-event-booking","ticket-api",Instant.now().plusSeconds(300));
        mvc.perform(get("/api/v1/tickets/by-booking/b1").header("Authorization","Bearer "+valid)).andExpect(status().isOk());
        for(String invalid:List.of(signedToken("wrong","ticket-api",Instant.now().plusSeconds(300)),signedToken("smart-event-booking","wrong",Instant.now().plusSeconds(300)),signedToken("smart-event-booking","ticket-api",Instant.now().minusSeconds(120)),valid.substring(0,valid.lastIndexOf('.')+1)+"invalid"))
            mvc.perform(get("/api/v1/tickets/by-booking/b1").header("Authorization","Bearer "+invalid)).andExpect(status().isUnauthorized());
    }
    @Test void listenerRejectsMalformedJsonAndWrongKey() throws Exception {
        assertThatThrownBy(()->listener.consume(new org.apache.kafka.clients.consumer.ConsumerRecord<>("booking.confirmed.v1",0,0,"b1","bad json"))).isInstanceOf(InvalidBookingEventException.class);
        assertThatThrownBy(()->listener.consume(new org.apache.kafka.clients.consumer.ConsumerRecord<>("booking.confirmed.v1",0,1,"wrong",mapper.writeValueAsString(event("m1","b1","c1",501L))))).isInstanceOf(InvalidBookingEventException.class);
        assertThat(count("tickets")).isZero();
    }
}
