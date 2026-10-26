package org.afterglow.paymentservice.controller;

import org.afterglow.paymentservice.model.Payment;
import org.afterglow.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // GET /api/v1/payments/{paymentId}
    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable String paymentId) {
        return paymentService.getPaymentById(paymentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // GET /api/v1/payments/by-booking/{bookingId}
    @GetMapping("/payments/by-booking/{bookingId}")
    public ResponseEntity<Payment> getPaymentByBookingId(@PathVariable String bookingId) {
        return paymentService.getPaymentByBookingId(bookingId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // POST /api/v1/payment
    @PostMapping("/payment")
    public ResponseEntity<Payment> createPayment(@RequestHeader("x-idempotency-key") String idempotencyKey,
                                                 @RequestBody Payment paymentRequest) {
        Payment createdPayment = paymentService.createPayment(paymentRequest, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPayment);
    }

    // POST /api/v1/payments/{paymentId}/simulate-decline
    @PostMapping("/payments/{paymentId}/simulate-decline")
    public ResponseEntity<Payment> simulateDeclinePayment(@PathVariable String paymentId,
                                                          @RequestHeader("x-idempotency-key") String idempotencyKey) {
        Payment declinedPayment = paymentService.simulateDeclinePayment(paymentId, idempotencyKey);
        return ResponseEntity.ok(declinedPayment);
    }
}
