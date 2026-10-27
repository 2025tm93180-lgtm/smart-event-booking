package org.afterglow.paymentservice;

import org.afterglow.paymentservice.model.OutboxEvent;
import org.afterglow.paymentservice.model.Payment;
import org.afterglow.paymentservice.model.PaymentStatusEnum;
import org.afterglow.paymentservice.producer.OutboxEventProducer;
import org.afterglow.paymentservice.repository.OutboxEventRepository;
import org.afterglow.paymentservice.repository.PaymentIdempotencyRepository;
import org.afterglow.paymentservice.repository.PaymentRepository;
import org.afterglow.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentIdempotencyRepository idempotencyRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxEventProducer outboxEventProducer;

    @InjectMocks
    private PaymentService paymentService;

    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        samplePayment = new Payment();
        samplePayment.setUuId("pay-uuid-123");
        samplePayment.setBookingId("booking-456");
        samplePayment.setAmount(100.0);
        samplePayment.setCurrency("INR");
        samplePayment.setStatus(PaymentStatusEnum.PENDING);
    }

    @Test
    void testCreatePaymentSuccess() {
        when(idempotencyRepository.findByIdempotencyKey("idempotency-key")).thenReturn(Optional.empty());
        when(paymentRepository.findByBookingId("booking-456")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setUuId("pay-uuid-123");
            return p;
        });
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.createPayment(samplePayment, "idempotency-key");

        assertNotNull(result);
        assertEquals("pay-uuid-123", result.getUuId());
        assertEquals("booking-456", result.getBookingId());
        assertEquals(PaymentStatusEnum.PENDING, result.getStatus());
        assertNotNull(result.getTimestamps());
        verify(paymentRepository, times(1)).save(samplePayment);
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
        verify(outboxEventProducer, times(1)).send(any(OutboxEvent.class));
    }

    @Test
    void testCreatePaymentUpdatesExistingPaymentByBookingId() {
        Payment existingPayment = new Payment();
        existingPayment.setUuId("pay-uuid-existing-999");
        existingPayment.setBookingId("booking-456");
        existingPayment.setAmount(100.0);
        existingPayment.setCurrency("INR");
        existingPayment.setStatus(PaymentStatusEnum.PENDING);

        org.afterglow.paymentservice.model.PaymentIdempotency existingIdempotency = new org.afterglow.paymentservice.model.PaymentIdempotency();
        existingIdempotency.setId(10L);
        existingIdempotency.setIdempotencyKey("idempotency-key-2");
        existingIdempotency.setResultStatus("PENDING");

        when(idempotencyRepository.findByIdempotencyKey("idempotency-key-2"))
                .thenReturn(Optional.empty()) // in findIdempotentReplay check
                .thenReturn(Optional.of(existingIdempotency)); // in saveIdempotencyRecord
        when(paymentRepository.findByBookingId("booking-456")).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment updatedPaymentRequest = new Payment();
        updatedPaymentRequest.setBookingId("booking-456");
        updatedPaymentRequest.setStatus(PaymentStatusEnum.CONFIRMED);
        updatedPaymentRequest.setRazorpayOrderId("order_abc");
        updatedPaymentRequest.setRazorpayPaymentId("pay_xyz");
        updatedPaymentRequest.setRazorpaySignature("sig_123");

        Payment result = paymentService.createPayment(updatedPaymentRequest, "idempotency-key-2");

        assertNotNull(result);
        assertEquals("pay-uuid-existing-999", result.getUuId());
        assertEquals("booking-456", result.getBookingId());
        assertEquals(PaymentStatusEnum.CONFIRMED, result.getStatus());
        assertEquals("order_abc", result.getRazorpayOrderId());
        assertEquals("pay_xyz", result.getRazorpayPaymentId());
        assertEquals("sig_123", result.getRazorpaySignature());
        verify(paymentRepository, times(1)).save(existingPayment);
        verify(idempotencyRepository, times(1)).save(existingIdempotency);
        assertEquals("CONFIRMED", existingIdempotency.getResultStatus());
        assertEquals("pay-uuid-existing-999", existingIdempotency.getPaymentId());
    }

    @Test
    void testCreatePaymentNullThrows() {
        assertThrows(ResponseStatusException.class, () -> paymentService.createPayment(null, "idempotency-key"));
        verify(paymentRepository, never()).save(any());
        verify(outboxEventProducer, never()).send(any());
    }

    @Test
    void testGetPaymentByIdFound() {
        when(paymentRepository.findById("pay-uuid-123")).thenReturn(Optional.of(samplePayment));

        Optional<Payment> result = paymentService.getPaymentById("pay-uuid-123");

        assertTrue(result.isPresent());
        assertEquals("pay-uuid-123", result.get().getUuId());
        verify(paymentRepository, times(1)).findById("pay-uuid-123");
    }

    @Test
    void testGetPaymentByIdNotFound() {
        when(paymentRepository.findById("non-existent")).thenReturn(Optional.empty());

        Optional<Payment> result = paymentService.getPaymentById("non-existent");

        assertFalse(result.isPresent());
        verify(paymentRepository, times(1)).findById("non-existent");
    }

    @Test
    void testGetPaymentByBookingIdFound() {
        when(paymentRepository.findByBookingId("booking-456")).thenReturn(Optional.of(samplePayment));

        Optional<Payment> result = paymentService.getPaymentByBookingId("booking-456");

        assertTrue(result.isPresent());
        assertEquals("booking-456", result.get().getBookingId());
        verify(paymentRepository, times(1)).findByBookingId("booking-456");
    }

    @Test
    void testSimulateDeclinePaymentSuccess() {
        when(idempotencyRepository.findByIdempotencyKey("idempotency-key")).thenReturn(Optional.empty());
        when(paymentRepository.findById("pay-uuid-123")).thenReturn(Optional.of(samplePayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.simulateDeclinePayment("pay-uuid-123", "idempotency-key");

        assertNotNull(result);
        assertEquals(PaymentStatusEnum.FAILED, result.getStatus());
        assertEquals("Declined: Simulated payment decline", result.getFailureReason());
        verify(paymentRepository, times(1)).save(samplePayment);
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
        verify(outboxEventProducer, times(1)).send(any(OutboxEvent.class));
    }

    @Test
    void testSimulateDeclinePaymentNotFound() {
        when(idempotencyRepository.findByIdempotencyKey("idempotency-key")).thenReturn(Optional.empty());
        when(paymentRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> paymentService.simulateDeclinePayment("non-existent", "idempotency-key"));
        verify(paymentRepository, never()).save(any());
        verify(outboxEventProducer, never()).send(any());
    }
}
