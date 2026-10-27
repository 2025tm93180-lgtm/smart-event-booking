package org.afterglow.paymentservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.afterglow.paymentservice.dto.CreateOrderRequest;
import org.afterglow.paymentservice.dto.CreateOrderResponse;
import org.afterglow.paymentservice.dto.VerifyPaymentRequest;
import org.afterglow.paymentservice.dto.VerifyPaymentResponse;
import org.afterglow.paymentservice.service.RazorpayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RazorpayCheckoutController {

    private final RazorpayService razorpayService;

    @Autowired
    public RazorpayCheckoutController(RazorpayService razorpayService) {
        this.razorpayService = razorpayService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<CreateOrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("Creating order for booking ID: {} at RazorpayCheckoutController", request.getReceipt());
        CreateOrderResponse response = razorpayService.createOrder(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<VerifyPaymentResponse> verifyPayment(@RequestBody VerifyPaymentRequest request) {
        log.info("Verifying payment for order ID: {} at RazorpayCheckoutController", request.getRazorpayOrderId());
        VerifyPaymentResponse response = razorpayService.verifyPayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        log.info("Fetching Razorpay configuration at RazorpayCheckoutController");
        return ResponseEntity.ok(Map.of("keyId", razorpayService.getKeyId() != null ? razorpayService.getKeyId() : ""));
    }
}
