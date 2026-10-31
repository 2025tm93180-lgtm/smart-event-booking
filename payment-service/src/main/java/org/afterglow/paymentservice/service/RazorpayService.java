package org.afterglow.paymentservice.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.afterglow.paymentservice.config.RazorpayConfig;
import org.afterglow.paymentservice.dto.CreateOrderRequest;
import org.afterglow.paymentservice.dto.CreateOrderResponse;
import org.afterglow.paymentservice.dto.VerifyPaymentRequest;
import org.afterglow.paymentservice.dto.VerifyPaymentResponse;
import org.afterglow.paymentservice.model.Payment;
import org.afterglow.paymentservice.model.PaymentStatusEnum;
import org.afterglow.paymentservice.repository.PaymentRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class RazorpayService {

    private final RazorpayConfig razorpayConfig;
    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;

    @Autowired
    public RazorpayService(RazorpayConfig razorpayConfig,
                           @Autowired(required = false) PaymentRepository paymentRepository,
                           @Autowired(required = false) RazorpayClient razorpayClient) {
        this.razorpayConfig = razorpayConfig;
        this.paymentRepository = paymentRepository;
        this.razorpayClient = razorpayClient;
    }

    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating Razorpay order for request: {} at RazorpayService", request);
        if (request == null || request.getAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount is required");
        }

        BigDecimal amount = request.getAmount();
        if (amount.compareTo(BigDecimal.valueOf(100)) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be at least 100 paise");
        }

        String currency = (request.getCurrency() != null && !request.getCurrency().isBlank())
                ? request.getCurrency()
                : "INR";

        String keySecret = razorpayConfig.getKeySecret();
        String keyId = razorpayConfig.getKeyId();
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Razorpay credentials not configured");
        }

        String bookingId = (request.getBookingId() != null && !request.getBookingId().isBlank())
                ? request.getBookingId()
                : request.getReceipt();
        try {
            RazorpayClient client = this.razorpayClient != null ? this.razorpayClient : new RazorpayClient(keyId, keySecret);

            if (bookingId != null && !bookingId.isBlank() && paymentRepository != null) {
                Optional<Payment> existingPaymentOpt = paymentRepository.findByBookingId(bookingId);
                if (existingPaymentOpt.isPresent()) {
                    Payment existingPayment = existingPaymentOpt.get();
                    if (existingPayment.getRazorpayOrderId() != null && !existingPayment.getRazorpayOrderId().isBlank()) {
                        Order existingOrder = client.orders.fetch(existingPayment.getRazorpayOrderId());
                        String orderId = existingOrder.get("id");
                        Number orderAmount = existingOrder.get("amount");
                        String orderCurrency = existingOrder.get("currency");

                        return new CreateOrderResponse(
                                orderId,
                                orderAmount != null ? BigDecimal.valueOf(orderAmount.longValue()) : amount,
                                orderCurrency != null ? orderCurrency : currency
                        );
                    }
                }
            }

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount.intValue());
            orderRequest.put("currency", currency);
            if (request.getReceipt() != null && !request.getReceipt().isBlank()) {
                orderRequest.put("receipt", request.getReceipt());
            }

            Order order = client.orders.create(orderRequest);
            String orderId = order.get("id");
            Number orderAmount = order.get("amount");
            String orderCurrency = order.get("currency");

            if (bookingId != null && !bookingId.isBlank() && paymentRepository != null) {
                Optional<Payment> existingPaymentOpt = paymentRepository.findByBookingId(bookingId);
                if (existingPaymentOpt.isPresent()) {
                    Payment payment = existingPaymentOpt.get();
                    payment.setRazorpayOrderId(orderId);
                    paymentRepository.save(payment);
                } else {
                    Payment payment = new Payment();
                    payment.setBookingId(bookingId);
                    payment.setRazorpayOrderId(orderId);
                    payment.setAmount(amount.doubleValue());
                    payment.setCurrency(currency);
                    payment.setStatus(PaymentStatusEnum.PENDING);
                    payment.setTimestamps(Instant.now().toString());
                    paymentRepository.save(payment);
                }
            }

            return new CreateOrderResponse(
                    orderId,
                    orderAmount != null ? BigDecimal.valueOf(orderAmount.longValue()) : amount,
                    orderCurrency != null ? orderCurrency : currency
            );
        } catch (RazorpayException e) {
            String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (message.contains("auth") || message.contains("unauthorized") || message.contains("key")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Razorpay authentication failed: " + e.getMessage(), e);
            }
            System.out.println("Razorpay error: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Razorpay error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error: " + e.getMessage(), e);
        }
    }

    public VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request) {
        log.info("Verifying Razorpay payment for request: {} at RazorpayService", request);
        if (request == null ||
                request.getRazorpayOrderId() == null || request.getRazorpayOrderId().isBlank() ||
                request.getRazorpayPaymentId() == null || request.getRazorpayPaymentId().isBlank() ||
                request.getRazorpaySignature() == null || request.getRazorpaySignature().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required verification fields");
        }

        String keySecret = razorpayConfig.getKeySecret();
        if (keySecret == null || keySecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Razorpay key secret not configured");
        }

        String data = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        String generatedSignature = calculateHmacSha256(data, keySecret);

        if (!generatedSignature.equalsIgnoreCase(request.getRazorpaySignature())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment signature verification failed");
        }

        return new VerifyPaymentResponse(true, "Payment verified successfully");
    }

    public String getKeyId() {
        return razorpayConfig.getKeyId();
    }

    public static String calculateHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to calculate HMAC SHA256", e);
        }
    }
}
