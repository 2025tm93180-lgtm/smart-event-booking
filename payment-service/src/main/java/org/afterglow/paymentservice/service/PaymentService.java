package org.afterglow.paymentservice.service;

import lombok.extern.slf4j.Slf4j;
import org.afterglow.paymentservice.model.Payment;
import org.afterglow.paymentservice.model.PaymentStatusEnum;
import org.afterglow.paymentservice.producer.OutboxEventProducer;
import org.afterglow.paymentservice.repository.PaymentRepository;
import org.afterglow.paymentservice.repository.PaymentIdempotencyRepository;
import org.afterglow.paymentservice.repository.OutboxEventRepository;
import org.afterglow.paymentservice.model.PaymentIdempotency;
import org.afterglow.paymentservice.model.OutboxEvent;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentIdempotencyRepository idempotencyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventProducer outboxEventProducer;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, PaymentIdempotencyRepository idempotencyRepository,
                          OutboxEventRepository outboxEventRepository,
                          @Autowired(required = false) OutboxEventProducer outboxEventProducer) {
        this.paymentRepository = paymentRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventProducer = outboxEventProducer;
    }

    @Transactional
    public Payment createPayment(Payment payment, String idempotencyKey) {
        log.info("Creating payment with idempotency key: {} and payment payload: {}", idempotencyKey, payment);
        if (payment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment request cannot be null");
        }
        validateIdempotencyKey(idempotencyKey);
        String requestHash = hashRequest(payment);
        Payment paymentToSave = isPaymentRecordExist(payment);

        Payment persisted = paymentRepository.save(paymentToSave);
        saveIdempotencyRecord(idempotencyKey, requestHash, persisted);
        saveOutboxEvent("PAYMENT_FINISHED", idempotencyKey, persisted, false);
        log.info("Payment created with idempotency key: {} and payment payload: {}", idempotencyKey, persisted);
        return persisted;
    }

    private Payment isPaymentRecordExist(Payment payment) {
        Payment paymentToSave = new Payment();
        if (payment.getBookingId() != null && !payment.getBookingId().isBlank()) {
            Optional<Payment> existingOpt = paymentRepository.findByBookingId(payment.getBookingId());
            if (existingOpt.isPresent()) {
                Payment existing = existingOpt.get();
                existing.setStatus(payment.getStatus());
                existing.setAmount(payment.getAmount());
                existing.setCurrency(payment.getCurrency());
                existing.setFailureReason(payment.getFailureReason());
                existing.setRazorpayPaymentId(payment.getRazorpayPaymentId());
                existing.setRazorpayOrderId(payment.getRazorpayOrderId());
                existing.setRazorpaySignature(payment.getRazorpaySignature());
                existing.setTimestamps(payment.getTimestamps());
                existing.setTimestamps(Instant.now().toString());
                paymentToSave = existing;
            }else {
                return payment;
            }
        }
        return paymentToSave;
    }

    private String hashRequest(Payment payment) {
        String canonical = String.join("\n",
                value(payment.getBookingId()), Double.toString(payment.getAmount()),
                value(payment.getCurrency()), value(payment.getRazorpayOrderId()),
                value(payment.getRazorpayPaymentId()), value(payment.getRazorpaySignature()));
        return hashRequest("CREATE_PAYMENT", canonical);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    public Optional<Payment> getPaymentById(String paymentId) {
        log.info("Fetching payment by ID: {} at PaymentService", paymentId);
        if (paymentId == null || paymentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment ID cannot be blank");
        }
        Optional<Payment> byId = paymentRepository.findById(paymentId);
        System.out.println("by: " + byId);
        return byId;
    }

    public Optional<Payment> getPaymentByBookingId(String bookingId) {
        log.info("Fetching payment by booking ID: {} at PaymentService", bookingId);
        if (bookingId == null || bookingId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking ID cannot be blank");
        }
        return paymentRepository.findByBookingId(bookingId);
    }

    @Transactional
    public Payment simulateDeclinePayment(String paymentId, String idempotencyKey) {
        log.info("Simulating decline payment for payment ID: {} and idempotency key: {} at PaymentService", paymentId, idempotencyKey);
        if (paymentId == null || paymentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment ID cannot be blank");
        }
        validateIdempotencyKey(idempotencyKey);
        String requestHash = hashRequest("SIMULATE_DECLINE", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found with id: " + paymentId));
        payment.setStatus(PaymentStatusEnum.FAILED);
        payment.setFailureReason("Declined: Simulated payment decline");
        payment.setTimestamps(Instant.now().toString());
        Payment persisted = paymentRepository.save(payment);

        saveIdempotencyRecord(idempotencyKey, requestHash, persisted);
        saveOutboxEvent("PAYMENT_DECLINED", idempotencyKey, persisted, true);
        return persisted;
    }

    private void saveIdempotencyRecord(String key, String requestHash, Payment payment) {
        log.info("Saving idempotency record for idempotency key: {} and payment: {} at PaymentService", key, payment);
        PaymentIdempotency record = idempotencyRepository.findByIdempotencyKey(key)
                .orElseGet(() -> {
                    PaymentIdempotency newRecord = new PaymentIdempotency();
                    newRecord.setIdempotencyKey(key);
                    return newRecord;
                });

        record.setRequestHash(requestHash);
        record.setPaymentId(payment.getUuId());
        record.setResultStatus(payment.getStatus() != null ? payment.getStatus().name() : null);
        record.setTimestamps(Instant.now().toString());
        idempotencyRepository.save(record);
        log.info("Idempotency record saved for idempotency key: {} and payment: {}", key, record);
    }

    private void saveOutboxEvent(String eventType, String key, Payment payment, boolean includeFailureReason) {
        log.info("Saving outbox event for event type: {}, idempotency key: {} and payment: {} at PaymentService", eventType, key, payment);
        JSONObject payload = new JSONObject();
        payload.put("paymentId", payment.getUuId());
        payload.put("bookingId", payment.getBookingId() == null ? JSONObject.NULL : payment.getBookingId());
        payload.put("status", payment.getStatus().name());
        if (includeFailureReason) payload.put("failureReason", payment.getFailureReason());

        OutboxEvent event = new OutboxEvent();
        event.setUuId(payment.getUuId());
        event.setEvent_type(eventType);
        event.setPayload(payload.toString());
        event.setIdempotencyKey(key);
        event.setCreated_at(Instant.now().toString());
        event.setAttempts(0);
        OutboxEvent savedEvent = outboxEventRepository.save(event);
        if (outboxEventProducer != null) {
            outboxEventProducer.send(savedEvent);
        }
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid Idempotency-Key header is required (max 128 characters)");
        }
    }

    private String hashRequest(String operation, String identifier) {
        try {
            String canonical = operation + "\n" + identifier;
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
