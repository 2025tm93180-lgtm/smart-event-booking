package org.afterglow.paymentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.afterglow.paymentservice.controller.PaymentController;
import org.afterglow.paymentservice.model.Payment;
import org.afterglow.paymentservice.model.PaymentStatusEnum;
import org.afterglow.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        samplePayment = new Payment();
        samplePayment.setUuId("pay-uuid-123");
        samplePayment.setBookingId("booking-456");
        samplePayment.setAmount(250.0);
        samplePayment.setCurrency("INR");
        samplePayment.setStatus(PaymentStatusEnum.PENDING);
        samplePayment.setTimestamps("2026-10-11T12:00:00Z");
    }

    @Test
    void testGetPaymentByIdFound() throws Exception {
        when(paymentService.getPaymentById("pay-uuid-123")).thenReturn(Optional.of(samplePayment));

        mockMvc.perform(get("/api/v1/payments/pay-uuid-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuId").value("pay-uuid-123"))
                .andExpect(jsonPath("$.bookingId").value("booking-456"))
                .andExpect(jsonPath("$.amount").value(250.0))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testGetPaymentByIdNotFound() throws Exception {
        when(paymentService.getPaymentById("non-existent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/payments/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetPaymentByBookingIdFound() throws Exception {
        when(paymentService.getPaymentByBookingId("booking-456")).thenReturn(Optional.of(samplePayment));

        mockMvc.perform(get("/api/v1/payments/by-booking/booking-456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuId").value("pay-uuid-123"))
                .andExpect(jsonPath("$.bookingId").value("booking-456"));
    }

    @Test
    void testGetPaymentByBookingIdNotFound() throws Exception {
        when(paymentService.getPaymentByBookingId("non-existent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/payments/by-booking/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreatePayment() throws Exception {
        when(paymentService.createPayment(any(Payment.class), any(String.class))).thenReturn(samplePayment);

        mockMvc.perform(post("/api/v1/payment")
                        .header("x-idempotency-key", "idempotency-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samplePayment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuId").value("pay-uuid-123"))
                .andExpect(jsonPath("$.bookingId").value("booking-456"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testSimulateDeclinePaymentSuccess() throws Exception {
        Payment declined = new Payment();
        declined.setUuId("pay-uuid-123");
        declined.setBookingId("booking-456");
        declined.setStatus(PaymentStatusEnum.FAILED);
        declined.setFailureReason("Declined: Simulated payment decline");

        when(paymentService.simulateDeclinePayment("pay-uuid-123", "idempotency-key")).thenReturn(declined);

        mockMvc.perform(post("/api/v1/payments/pay-uuid-123/simulate-decline")
                        .header("x-idempotency-key", "idempotency-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Declined: Simulated payment decline"));
    }

    @Test
    void testSimulateDeclinePaymentNotFound() throws Exception {
        when(paymentService.simulateDeclinePayment("non-existent", "idempotency-key"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        mockMvc.perform(post("/api/v1/payments/non-existent/simulate-decline")
                        .header("x-idempotency-key", "idempotency-key"))
                .andExpect(status().isNotFound());
    }
}
