package com.team.ticketnotification;
import com.team.ticketnotification.messaging.BookingConfirmedEvent;
import com.team.ticketnotification.ticket.TicketIssuanceService;
import com.team.ticketnotification.notification.NotificationWorker;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker=true)
class MySqlConcurrencyTest {
    @Container static MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",mysql::getJdbcUrl);r.add("spring.datasource.username",mysql::getUsername);r.add("spring.datasource.password",mysql::getPassword);
    }
    @Autowired TicketIssuanceService service;
    @Autowired JdbcTemplate db;
    @Autowired TransactionTemplate transactions;
    @Autowired com.team.ticketnotification.notification.NotificationDeliveryStore deliveryStore;
    @Test void mysqlLocksUniqueConstraintsAndWorkerAreCompatible() throws Exception {
        try(var pool=Executors.newFixedThreadPool(8)) {
            var calls=new ArrayList<Callable<Boolean>>();
            for(int i=0;i<20;i++) {String id="message-"+i;calls.add(()->service.issue(new BookingConfirmedEvent(id,1,"booking.confirmed.v1","corr-mysql",Instant.now(),"booking-mysql",new BookingConfirmedEvent.Payload("c1",101L,Instant.now(),List.of(new BookingConfirmedEvent.Item(501L),new BookingConfirmedEvent.Item(502L))))));}
            int created=0;for(var result:pool.invokeAll(calls)) if(result.get(30,TimeUnit.SECONDS))created++;
            assertThat(created).isEqualTo(1);
        }
        assertThat(db.queryForObject("SELECT COUNT(*) FROM tickets",Long.class)).isEqualTo(2);
        new NotificationWorker(deliveryStore,Optional.empty(),false,20).deliverPending();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM notification_attempts WHERE status='SENT'",Long.class)).isEqualTo(2);
    }
}
