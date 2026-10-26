package org.afterglow.paymentservice.producer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.afterglow.paymentservice.model.OutboxEvent;
import org.afterglow.paymentservice.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class OutboxEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxEventRepository outboxEventRepository;
    @Getter
    private final String topic;

    @Autowired
    public OutboxEventProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            OutboxEventRepository outboxEventRepository,
            @Value("${spring.kafka.topic.payment-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxEventRepository = outboxEventRepository;
        this.topic = topic;
    }

    public CompletableFuture<SendResult<String, String>> produce(OutboxEvent outboxEvent) {
        return send(this.topic, outboxEvent);
    }

    public CompletableFuture<SendResult<String, String>> send(OutboxEvent outboxEvent) {
        return send(this.topic, outboxEvent);
    }

    public CompletableFuture<SendResult<String, String>> produceOutboxEvent(OutboxEvent outboxEvent) {
        return send(this.topic, outboxEvent);
    }

    public CompletableFuture<SendResult<String, String>> send(String topic, OutboxEvent outboxEvent) {
        if (outboxEvent == null) {
            log.warn("Attempted to produce null OutboxEvent");
            return CompletableFuture.completedFuture(null);
        }
        String targetTopic = (topic != null && !topic.isBlank()) ? topic : this.topic;
        String key = outboxEvent.getUuId() != null ? outboxEvent.getUuId() : outboxEvent.getEvent_id();
        String data = outboxEvent.getPayload() != null ? outboxEvent.getPayload() : "";

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(targetTopic, key, data);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully produced OutboxEvent with event_id: {} to topic: {}", outboxEvent.getEvent_id(), targetTopic);
                outboxEvent.setPublished_at(Instant.now().toString());
                outboxEvent.setAttempts(outboxEvent.getAttempts() + 1);
                if (outboxEventRepository != null) {
                    outboxEventRepository.save(outboxEvent);
                }
            } else {
                log.error("Failed to produce OutboxEvent with event_id: {} to topic: {}", outboxEvent.getEvent_id(), targetTopic, ex);
                outboxEvent.setAttempts(outboxEvent.getAttempts() + 1);
                if (outboxEventRepository != null) {
                    outboxEventRepository.save(outboxEvent);
                }
            }
        });

        return future;
    }

}


/*
*
* ------------- Happy path -------------
*
* 1 --> user login at browser level to store his name, phone number, mailAddress, dob
* 2 --> user selects an event under the catalog which event can be running today or in the future.
*       internally:
*           1. the events should get fetched from the event service. events should be running from today onwards. NO older events
*           2. the events should be filtered based on the date and time.
*           3. the events should be sorted based on the date and time.
* 3 --> user selects one or more seats for the selected event.
*       internally:
*           1. the seats should get fetched from the event service.
*           2. One API response will have the reserved seats, capacity of the auditorium
*           3. Another API response will have temporarily blocked seats. (or we can make it one API response)
*           3. the UI should block reserved seats and show available seats.
* * 4 --> once the user selects seats, they should get blocked temporarily for at least 10 minutes
*       internally:
*           1. UI should make an API call to temporarily block the selected seats for 10 minutes.
*           2. so no other user can select the same seat(s), after we blocked.
*           3. event service should remove the temporary block after 10 minutes.
* 5 --> once the user clicks on checkout, the UI should validate whether there are user details saved in UI.
*       internally:
*           1. UI should verify whether the user state is available or not.
* 6 --> if not user should redirect to user form page
* 7 --> if yes, then the user should be redirected to the payment page
* 8 --> once payment is successful, the user should be redirected to the confirmation page
* 9 --> in background, the payment service should publish an event to the payment_outbox_topic. so, booking service will make it lock for the duration of the event.
* 10 -->booking service will publish an event to the booking_outbox_topic. so, the notification service will send the confirmation email.
*
*
* -------------- partial booking flow --------------
*
* 1 --> user login at browser level to store his name, phone number, mailAddress, dob
* 2 --> user selects an event under the catalog which event can be running today or in the future.
*       internally:
*           1. the events should get fetched from the event service. events should be running from today onwards. NO older events
*           2. the events should be filtered based on the date and time.
*           3. the events should be sorted based on the date and time.
* 3 --> user selects one or more seats for the selected event.
*       internally:
*           1. the seats should get fetched from the event service.
*           2. One API response will have the reserved seats, capacity of the auditorium
*           3. Another API response will have temporarily blocked seats. (or we can make it one API response)
*           3. the UI should block reserved seats and show available seats.
* 4 --> once the user selects seats, they should get blocked temporarily for at least 10 minutes (after clicking on the checkout button)
*       internally:
*           1. UI should make an API call to temporarily block the selected seats for 10 minutes.
*           2. so no other user can select the same seat(s), after we blocked.
*           3. event service should remove the temporary block after 10 minutes.
* 5 --> once the user clicks on checkout, the UI should validate whether there are user details saved in the UI.
*       internally:
*           1. UI should verify whether the user state is available or not.
* 6 --> if not, the user should redirect to user form page
* 7 --> if yes, then the user should be redirected to the payment page
* 8 --> Now, users try to ignore the payment page and close the browser or refresh the page. by accepting ignorance of the payment.
* 9 --> if the user ignores the payment page, then selected seats will remain blocked for until the next allocated time.
*
*
* -------------- if 2 or more users tried to book the same seat --------------
*
* 1 --> user login at browser level to store his name, phone number, mailAddress, dob
* 2 --> user selects an event under the catalog which event can be running today or in the future.
*       internally:
*           1. the events should get fetched from the event service. events should be running from today onwards. NO older events
*           2. the events should be filtered based on the date and time.
*           3. the events should be sorted based on the date and time.
* 3 --> user selects one or more seats for the selected event.
*       internally:
*           1. the seats should get fetched from the event service.
*           2. One API response will have the reserved seats, capacity of the auditorium
*           3. Another API response will have temporarily blocked seats. (or we can make it one API response)
*           3. the UI should block reserved seats and show available seats.
* 4 --> once the user selects seats, they should get blocked temporarily for at least 10 minutes (after clicking on the checkout button)
*       internally:
*           1. UI should make an API call to temporarily block the selected seats for 10 minutes.
*           2. so no other user can select the same seat(s), after we blocked.
*           3. event service should remove the temporary block after 10 minutes.
*       possible case:
*           1. if they already got blocked at the same time, user will get an error message about seat unavailability.
*           2. I have a proper implementation idea for this case.
*  5 --> the outcome would one successful booking will happen instead of duplicate bookings.
*
* 10 exipred
*
*1. bookings -> updates exipred status updates the exipred seats
*2. booking-service scheduledJob updates the exipred seats
*
*
* */
