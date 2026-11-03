package org.afterglow.paymentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.afterglow.paymentservice.dto.BookingEvent;
import org.afterglow.paymentservice.listener.BookingEventListener;
import org.afterglow.paymentservice.model.InboxMessage;
import org.afterglow.paymentservice.model.Payment;
import org.afterglow.paymentservice.model.PaymentStatusEnum;
import org.afterglow.paymentservice.repository.InboxMessageRepository;
import org.afterglow.paymentservice.repository.PaymentRepository;
import org.afterglow.paymentservice.service.BookingEventProcessingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingEventListenerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InboxMessageRepository inboxMessageRepository;

    private ObjectMapper objectMapper;
    private BookingEventProcessingService processingService;
    private BookingEventListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        processingService = new BookingEventProcessingService(paymentRepository, inboxMessageRepository);
        listener = new BookingEventListener(objectMapper, processingService);
    }

    @Test
    void testConsumeConsumerRecordSuccess() {
        String payload = """
                {
                  "bookingId": "booking-123",
                  "eventId": "event-456",
                  "seatIds": ["s1", "s2"],
                  "totalAmount": 250.0,
                  "currency": "INR"
                }
                """;

        when(paymentRepository.findByBookingId("booking-123")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inboxMessageRepository.save(any(InboxMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConsumerRecord<String, String> record = new ConsumerRecord<>("booking-events", 0, 0L, "booking-123", payload);
        listener.consume(record);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();
        assertEquals("booking-123", savedPayment.getBookingId());
        assertEquals(250.0, savedPayment.getAmount());
        assertEquals("INR", savedPayment.getCurrency());
        assertEquals(PaymentStatusEnum.PENDING, savedPayment.getStatus());
        assertNotNull(savedPayment.getTimestamps());

        verify(inboxMessageRepository, times(1)).save(any(InboxMessage.class));
    }

    @Test
    void testConsumeUsesRecordKeyWhenBookingIdMissingInPayload() {
        String payload = """
                {
                  "eventId": "event-456",
                  "totalAmount": 150.0,
                  "currency": "USD"
                }
                """;

        when(paymentRepository.findByBookingId("key-booking-789")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inboxMessageRepository.save(any(InboxMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConsumerRecord<String, String> record = new ConsumerRecord<>("booking-events", 0, 0L, "key-booking-789", payload);
        listener.consume(record);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();
        assertEquals("key-booking-789", savedPayment.getBookingId());
        assertEquals(150.0, savedPayment.getAmount());
        assertEquals("USD", savedPayment.getCurrency());
    }

    @Test
    void testConsumeStringMessageSuccess() {
        String payload = """
                {
                  "bookingId": "booking-999",
                  "eventId": "event-111",
                  "seatIds": ["s3"],
                  "amount": 500.0,
                  "currency": "EUR"
                }
                """;

        when(paymentRepository.findByBookingId("booking-999")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inboxMessageRepository.save(any(InboxMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        listener.consume(payload);

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(inboxMessageRepository, times(1)).save(any(InboxMessage.class));
    }

    @Test
    void testConsumeUpdatesExistingPayment() {
        String payload = """
                {
                  "bookingId": "existing-booking",
                  "totalAmount": 300.0,
                  "currency": "INR"
                }
                """;

        Payment existingPayment = new Payment();
        existingPayment.setUuId("pay-uuid-existing");
        existingPayment.setBookingId("existing-booking");
        existingPayment.setAmount(0.0);
        existingPayment.setStatus(PaymentStatusEnum.PENDING);

        when(paymentRepository.findByBookingId("existing-booking")).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inboxMessageRepository.save(any(InboxMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        listener.consume(payload);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();
        assertEquals("pay-uuid-existing", savedPayment.getUuId());
        assertEquals(300.0, savedPayment.getAmount());
        assertEquals("INR", savedPayment.getCurrency());
    }

    @Test
    void testConsumeNullOrBlankPayloadDoesNotThrow() {
        ConsumerRecord<String, String> nullValueRecord = new ConsumerRecord<>("booking-events", 0, 0L, "key", null);
        assertDoesNotThrow(() -> listener.consume(nullValueRecord));

        ConsumerRecord<String, String> blankValueRecord = new ConsumerRecord<>("booking-events", 0, 0L, "key", "   ");
        assertDoesNotThrow(() -> listener.consume(blankValueRecord));

        assertDoesNotThrow(() -> listener.consume((ConsumerRecord<String, String>) null));
        assertDoesNotThrow(() -> listener.consume(""));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testConsumeInvalidJsonThrowsRuntimeException() {
        ConsumerRecord<String, String> invalidRecord = new ConsumerRecord<>("booking-events", 0, 0L, "key", "{ invalid json");
        assertThrows(RuntimeException.class, () -> listener.consume(invalidRecord));
    }
}
