package org.afterglow.paymentservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.afterglow.paymentservice.model.Payment;
import org.afterglow.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable String paymentId) {
        log.info("Fetching payment with ID: {} at PaymentController", paymentId);
        return paymentService.getPaymentById(paymentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/payments/by-booking/{bookingId}")
    public ResponseEntity<Payment> getPaymentByBookingId(@PathVariable String bookingId) {
        log.info("Fetching payment by booking ID: {} at PaymentController", bookingId);
        return paymentService.getPaymentByBookingId(bookingId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/payment")
    public ResponseEntity<Payment> createPayment(@RequestHeader("x-idempotency-key") String idempotencyKey,
                                                 @RequestBody Payment paymentRequest) {
        log.info("Creating payment with idempotency key: {} at PaymentController", idempotencyKey);
        Payment createdPayment = paymentService.createPayment(paymentRequest, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPayment);
    }

    @PostMapping("/payments/{paymentId}/simulate-decline")
    public ResponseEntity<Payment> simulateDeclinePayment(@PathVariable String paymentId,
                                                          @RequestHeader("x-idempotency-key") String idempotencyKey) {
        log.info("Simulating decline for payment with ID: {} and idempotency key: {} at PaymentController", paymentId, idempotencyKey);
        Payment declinedPayment = paymentService.simulateDeclinePayment(paymentId, idempotencyKey);
        return ResponseEntity.ok(declinedPayment);
    }
}
