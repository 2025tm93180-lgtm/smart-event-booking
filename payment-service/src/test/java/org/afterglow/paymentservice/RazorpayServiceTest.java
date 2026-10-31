package org.afterglow.paymentservice;

import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import org.afterglow.paymentservice.config.RazorpayConfig;
import org.afterglow.paymentservice.dto.CreateOrderRequest;
import org.afterglow.paymentservice.dto.CreateOrderResponse;
import org.afterglow.paymentservice.dto.VerifyPaymentRequest;
import org.afterglow.paymentservice.dto.VerifyPaymentResponse;
import org.afterglow.paymentservice.model.Payment;
import org.afterglow.paymentservice.repository.PaymentRepository;
import org.afterglow.paymentservice.service.RazorpayService;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RazorpayServiceTest {

    private RazorpayService razorpayService;
    private RazorpayConfig razorpayConfig;
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        razorpayConfig = new RazorpayConfig();
        ReflectionTestUtils.setField(razorpayConfig, "keyId", "rzp_test_TmWw6lp6USWewH");
        ReflectionTestUtils.setField(razorpayConfig, "keySecret", "8WbHmNwSmJGTtLpUUcAPMCJH");
        paymentRepository = mock(PaymentRepository.class);
        razorpayService = new RazorpayService(razorpayConfig, paymentRepository, null);
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
    void testCreateOrderWithExistingBookingIdFetchesExistingOrder() throws Exception {
        RazorpayClient mockClient = mock(RazorpayClient.class);
        OrderClient mockOrders = mock(OrderClient.class);
        ReflectionTestUtils.setField(mockClient, "orders", mockOrders);

        Payment existingPayment = new Payment();
        existingPayment.setBookingId("booking-12345");
        existingPayment.setRazorpayOrderId("order_existing_123");
        existingPayment.setAmount(79900.0);
        existingPayment.setCurrency("INR");

        when(paymentRepository.findByBookingId("booking-12345")).thenReturn(Optional.of(existingPayment));

        JSONObject orderJson = new JSONObject();
        orderJson.put("id", "order_existing_123");
        orderJson.put("amount", 79900);
        orderJson.put("currency", "INR");
        Order fetchedOrder = new Order(orderJson);

        when(mockOrders.fetch("order_existing_123")).thenReturn(fetchedOrder);

        RazorpayService service = new RazorpayService(razorpayConfig, paymentRepository, mockClient);

        CreateOrderRequest request = new CreateOrderRequest("booking-12345", BigDecimal.valueOf(79900), "INR", "AF-12345");
        CreateOrderResponse response = service.createOrder(request);

        assertNotNull(response);
        assertEquals("order_existing_123", response.getOrder_id());
        assertEquals(BigDecimal.valueOf(79900), response.getAmount());
        assertEquals("INR", response.getCurrency());

        verify(mockOrders, times(1)).fetch("order_existing_123");
        verify(mockOrders, never()).create(any(JSONObject.class));
    }

    @Test
    void testCreateOrderNewBookingCreatesAndSavesPayment() throws Exception {
        RazorpayClient mockClient = mock(RazorpayClient.class);
        OrderClient mockOrders = mock(OrderClient.class);
        ReflectionTestUtils.setField(mockClient, "orders", mockOrders);

        when(paymentRepository.findByBookingId("booking-new-999")).thenReturn(Optional.empty());

        JSONObject orderJson = new JSONObject();
        orderJson.put("id", "order_new_999");
        orderJson.put("amount", 79900);
        orderJson.put("currency", "INR");
        Order createdOrder = new Order(orderJson);

        when(mockOrders.create(any(JSONObject.class))).thenReturn(createdOrder);

        RazorpayService service = new RazorpayService(razorpayConfig, paymentRepository, mockClient);

        CreateOrderRequest request = new CreateOrderRequest("booking-new-999", BigDecimal.valueOf(79900), "INR", "AF-12345");
        CreateOrderResponse response = service.createOrder(request);

        assertNotNull(response);
        assertEquals("order_new_999", response.getOrder_id());
        assertEquals(BigDecimal.valueOf(79900), response.getAmount());
        assertEquals("INR", response.getCurrency());

        verify(mockOrders, times(1)).create(any(JSONObject.class));
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }
}
