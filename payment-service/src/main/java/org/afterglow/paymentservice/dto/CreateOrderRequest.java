package org.afterglow.paymentservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class CreateOrderRequest {
    private String bookingId;
    private BigDecimal amount;
    private String currency;
    private String receipt;

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(BigDecimal amount, String currency, String receipt) {
        this.amount = amount;
        this.currency = currency;
        this.receipt = receipt;
    }

    public CreateOrderRequest(String bookingId, BigDecimal amount, String currency, String receipt) {
        this.bookingId = bookingId;
        this.amount = amount;
        this.currency = currency;
        this.receipt = receipt;
    }
}
