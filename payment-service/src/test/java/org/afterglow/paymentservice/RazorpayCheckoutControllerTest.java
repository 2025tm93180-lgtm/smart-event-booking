package org.afterglow.paymentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.afterglow.paymentservice.controller.RazorpayCheckoutController;
import org.afterglow.paymentservice.dto.CreateOrderRequest;
import org.afterglow.paymentservice.dto.CreateOrderResponse;
import org.afterglow.paymentservice.dto.VerifyPaymentRequest;
import org.afterglow.paymentservice.dto.VerifyPaymentResponse;
import org.afterglow.paymentservice.service.RazorpayService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RazorpayCheckoutController.class)
class RazorpayCheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RazorpayService razorpayService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateOrderEndpointSuccess() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(BigDecimal.valueOf(50000), "INR", "receipt#1");
        CreateOrderResponse response = new CreateOrderResponse("order_12345", BigDecimal.valueOf(50000), "INR");

        Mockito.when(razorpayService.createOrder(any())).thenReturn(response);

        mockMvc.perform(post("/api/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_id").value("order_12345"))
                .andExpect(jsonPath("$.amount").value(50000))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void testCreateOrderEndpointValidationError() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(BigDecimal.valueOf(50), "INR", "receipt#1");

        Mockito.when(razorpayService.createOrder(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be at least 100 paise"));

        mockMvc.perform(post("/api/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testVerifyPaymentEndpointSuccess() throws Exception {
        VerifyPaymentRequest request = new VerifyPaymentRequest("order_123", "pay_456", "signature_abc");
        VerifyPaymentResponse response = new VerifyPaymentResponse(true, "Payment verified successfully");

        Mockito.when(razorpayService.verifyPayment(any())).thenReturn(response);

        mockMvc.perform(post("/api/verify-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment verified successfully"));
    }

    @Test
    void testVerifyPaymentEndpointInvalidSignature() throws Exception {
        VerifyPaymentRequest request = new VerifyPaymentRequest("order_123", "pay_456", "bad_signature");

        Mockito.when(razorpayService.verifyPayment(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment signature verification failed"));

        mockMvc.perform(post("/api/verify-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetConfigEndpoint() throws Exception {
        Mockito.when(razorpayService.getKeyId()).thenReturn("rzp_test_TmWw6lp6USWewH");

        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyId").value("rzp_test_TmWw6lp6USWewH"));
    }
}
