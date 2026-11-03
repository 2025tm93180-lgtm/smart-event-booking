package org.afterglow.paymentservice.service;

import lombok.extern.slf4j.Slf4j;
import org.afterglow.paymentservice.dto.BookingEvent;
import org.afterglow.paymentservice.model.InboxMessage;
import org.afterglow.paymentservice.model.Payment;
import org.afterglow.paymentservice.model.PaymentStatusEnum;
import org.afterglow.paymentservice.repository.InboxMessageRepository;
import org.afterglow.paymentservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class BookingEventProcessingService {

    private final PaymentRepository paymentRepository;
    private final InboxMessageRepository inboxMessageRepository;

    @Autowired
    public BookingEventProcessingService(PaymentRepository paymentRepository,
                                         InboxMessageRepository inboxMessageRepository) {
        this.paymentRepository = paymentRepository;
        this.inboxMessageRepository = inboxMessageRepository;
    }

    @Transactional
    public Payment process(BookingEvent event, String key) {
        log.info("Processing booking event: {} with key: {}", event, key);
        if (event == null) {
            log.warn("Booking event is null, skipping processing");
            return null;
        }

        String bookingId = event.getBookingId() != null && !event.getBookingId().isBlank()
                ? event.getBookingId()
                : key;

        recordInboxMessage(event, bookingId);

        if (bookingId == null || bookingId.isBlank()) {
            log.warn("No bookingId found in event or key, skipping payment initialization");
            return null;
        }

        return initializeOrUpdatePayment(event, bookingId);
    }

    private void recordInboxMessage(BookingEvent event, String bookingId) {
        try {
            InboxMessage inboxMessage = new InboxMessage();
            if (event.getMessageId() != null && !event.getMessageId().isBlank()) {
                inboxMessage.setMessage_id(event.getMessageId());
            } else {
                inboxMessage.setMessage_id(UUID.randomUUID().toString());
            }

            String messageType = event.getEventType() != null && !event.getEventType().isBlank()
                    ? event.getEventType()
                    : "BOOKING_CREATED";
            inboxMessage.setMessage_type(messageType);
            inboxMessage.setProcessed_at(Instant.now().toString());

            inboxMessageRepository.save(inboxMessage);
            log.info("Recorded inbox message with id: {} and type: {}", inboxMessage.getMessage_id(), messageType);
        } catch (Exception e) {
            log.error("Failed to record inbox message for booking: {}", bookingId, e);
        }
    }

    private Payment initializeOrUpdatePayment(BookingEvent event, String bookingId) {
        Optional<Payment> existingPaymentOpt = paymentRepository.findByBookingId(bookingId);
        Payment payment;
        if (existingPaymentOpt.isPresent()) {
            payment = existingPaymentOpt.get();
            log.info("Found existing payment for bookingId: {}", bookingId);
            if (payment.getAmount() == 0 && event.getEffectiveAmount() > 0) {
                payment.setAmount(event.getEffectiveAmount());
            }
            if ((payment.getCurrency() == null || payment.getCurrency().isBlank()) && event.getCurrency() != null) {
                payment.setCurrency(event.getCurrency());
            }
            if (payment.getStatus() == null) {
                payment.setStatus(PaymentStatusEnum.PENDING);
            }
        } else {
            payment = new Payment();
            payment.setBookingId(bookingId);
            payment.setAmount(event.getEffectiveAmount());
            payment.setCurrency(event.getCurrency() != null && !event.getCurrency().isBlank() ? event.getCurrency() : "INR");
            payment.setStatus(PaymentStatusEnum.PENDING);
            payment.setTimestamps(Instant.now().toString());
            log.info("Initialized new pending payment for bookingId: {}", bookingId);
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Saved payment record for bookingId: {} with paymentId: {}", bookingId, saved.getUuId());
        return saved;
    }
}
