package org.afterglow.paymentservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class CreateOrderRequest {
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

}
