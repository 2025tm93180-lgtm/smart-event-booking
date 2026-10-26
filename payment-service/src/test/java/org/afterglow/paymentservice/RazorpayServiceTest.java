package org.afterglow.paymentservice;

import org.afterglow.paymentservice.config.RazorpayConfig;
import org.afterglow.paymentservice.dto.CreateOrderRequest;
import org.afterglow.paymentservice.dto.VerifyPaymentRequest;
import org.afterglow.paymentservice.dto.VerifyPaymentResponse;
import org.afterglow.paymentservice.service.RazorpayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RazorpayServiceTest {

    private RazorpayService razorpayService;
    private RazorpayConfig razorpayConfig;

    @BeforeEach
    void setUp() {
        razorpayConfig = new RazorpayConfig();
        ReflectionTestUtils.setField(razorpayConfig, "keyId", "rzp_test_TmWw6lp6USWewH");
        ReflectionTestUtils.setField(razorpayConfig, "keySecret", "8WbHmNwSmJGTtLpUUcAPMCJH");
        razorpayService = new RazorpayService(razorpayConfig, null);
    }

    @Test
    void testCreateOrderValidationAmountTooLow() {
        CreateOrderRequest request = new CreateOrderRequest(BigDecimal.valueOf(50), "INR", "rcpt_1");
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            razorpayService.createOrder(request);
        });
        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Amount must be at least 100 paise"));
    }

    @Test
    void testCreateOrderValidationMissingAmount() {
        CreateOrderRequest request = new CreateOrderRequest(null, "INR", "rcpt_1");
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            razorpayService.createOrder(request);
        });
        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void testSignatureVerificationSuccess() {
        String orderId = "order_N123456789";
        String paymentId = "pay_N987654321";
        String secret = "8WbHmNwSmJGTtLpUUcAPMCJH";

        String expectedSignature = RazorpayService.calculateHmacSha256(orderId + "|" + paymentId, secret);

        VerifyPaymentRequest request = new VerifyPaymentRequest(orderId, paymentId, expectedSignature);
        VerifyPaymentResponse response = razorpayService.verifyPayment(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
    }

    @Test
    void testSignatureVerificationMismatch() {
        String orderId = "order_N123456789";
        String paymentId = "pay_N987654321";

        VerifyPaymentRequest request = new VerifyPaymentRequest(orderId, paymentId, "invalid_signature");
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            razorpayService.verifyPayment(request);
        });
        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void testSignatureVerificationMissingFields() {
        VerifyPaymentRequest request = new VerifyPaymentRequest(null, "pay_123", "sig");
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            razorpayService.verifyPayment(request);
        });
        assertEquals(400, exception.getStatusCode().value());
    }
}
