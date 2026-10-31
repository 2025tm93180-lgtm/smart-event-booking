package com.team.ticketnotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.ticketnotification.messaging.BookingConfirmedEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@SpringBootTest(properties={"app.kafka.health-enabled=true","spring.kafka.listener.auto-startup=true","spring.datasource.url=jdbc:h2:mem:kafkatest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"})
@ActiveProfiles("test")
@EmbeddedKafka(kraft=true,partitions=3,topics={"booking.confirmed.v1","booking.confirmed.v1.DLT"},bootstrapServersProperty="spring.kafka.bootstrap-servers")
@DirtiesContext
class KafkaFlowTest {
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired KafkaTemplate<String,String> producer;
    @Autowired JdbcTemplate db;
    @Autowired ObjectMapper mapper;
    @Autowired EmbeddedKafkaBroker broker;
    @Test void realListenerDeduplicatesAndRecoversPastPoisonMessage() throws Exception {
        var props=KafkaTestUtils.consumerProps("dlt-verification","false",broker);
        props.put("key.deserializer",StringDeserializer.class);props.put("value.deserializer",StringDeserializer.class);
        try(var dlt=new KafkaConsumer<String,String>(props)) {
            broker.consumeFromAnEmbeddedTopic(dlt,"booking.confirmed.v1.DLT");
            // Same partition: the valid record after malformed JSON proves the listener advances after recovery.
            producer.send("booking.confirmed.v1",0,"kafka-booking","not-json").get();
            var event=new BookingConfirmedEvent("kafka-event",1,"booking.confirmed.v1","corr-kafka",Instant.now(),"kafka-booking",new BookingConfirmedEvent.Payload("customer-001",101L,Instant.now(),List.of(new BookingConfirmedEvent.Item(501L),new BookingConfirmedEvent.Item(502L))));
            String json=mapper.writeValueAsString(event);
            producer.send("booking.confirmed.v1",0,event.bookingId(),json).get();
            producer.send("booking.confirmed.v1",0,event.bookingId(),json).get();
            await().atMost(Duration.ofSeconds(30)).untilAsserted(()->{
                assertThat(db.queryForObject("SELECT COUNT(*) FROM tickets",Long.class)).isEqualTo(2);
                assertThat(db.queryForObject("SELECT COUNT(*) FROM inbox_messages",Long.class)).isEqualTo(1);
            });
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/health"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("UP"));
            var records=KafkaTestUtils.getRecords(dlt,Duration.ofSeconds(20));
            assertThat(records.count()).isGreaterThanOrEqualTo(1);
            var dead=records.iterator().next();
            assertThat(dead.value()).isEqualTo("not-json");
            assertThat(dead.headers().lastHeader("kafka_dlt-original-topic")).isNotNull();
        }
    }
}
