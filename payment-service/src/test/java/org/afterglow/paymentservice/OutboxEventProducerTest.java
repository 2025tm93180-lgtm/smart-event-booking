package org.afterglow.paymentservice;

import org.afterglow.paymentservice.model.OutboxEvent;
import org.afterglow.paymentservice.producer.OutboxEventProducer;
import org.afterglow.paymentservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OutboxEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new OutboxEventProducer(kafkaTemplate, outboxEventRepository, "payment-events");
    }

    @Test
    void testProduceOutboxEventSuccess() {
        OutboxEvent event = new OutboxEvent();
        event.setEvent_id("event-123");
        event.setUuId("pay-123");
        event.setEvent_type("PAYMENT_FINISHED");
        event.setPayload("{\"paymentId\":\"pay-123\",\"status\":\"PENDING\"}");
        event.setIdempotencyKey("corr-123");
        event.setAttempts(0);

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("payment-events"), eq("pay-123"), eq("{\"paymentId\":\"pay-123\",\"status\":\"PENDING\"}")))
                .thenReturn(future);

        CompletableFuture<SendResult<String, String>> resultFuture = producer.send(event);
        future.complete(null);

        assertNotNull(resultFuture);
        assertNotNull(event.getPublished_at());
        assertEquals(1, event.getAttempts());
        verify(kafkaTemplate, times(1)).send("payment-events", "pay-123", "{\"paymentId\":\"pay-123\",\"status\":\"PENDING\"}");
        verify(outboxEventRepository, times(1)).save(event);
    }

    @Test
    void testProduceOutboxEventFailure() {
        OutboxEvent event = new OutboxEvent();
        event.setEvent_id("event-123");
        event.setUuId("pay-123");
        event.setEvent_type("PAYMENT_DECLINED");
        event.setPayload("{\"paymentId\":\"pay-123\",\"status\":\"FAILED\"}");
        event.setAttempts(0);

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("payment-events"), eq("pay-123"), eq("{\"paymentId\":\"pay-123\",\"status\":\"FAILED\"}")))
                .thenReturn(future);

        CompletableFuture<SendResult<String, String>> resultFuture = producer.produce(event);
        future.completeExceptionally(new RuntimeException("Kafka error"));

        assertNotNull(resultFuture);
        assertNull(event.getPublished_at());
        assertEquals(1, event.getAttempts());
        verify(kafkaTemplate, times(1)).send("payment-events", "pay-123", "{\"paymentId\":\"pay-123\",\"status\":\"FAILED\"}");
        verify(outboxEventRepository, times(1)).save(event);
    }

    @Test
    void testProduceNullEvent() {
        CompletableFuture<SendResult<String, String>> result = producer.send(null);
        assertNotNull(result);
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}
