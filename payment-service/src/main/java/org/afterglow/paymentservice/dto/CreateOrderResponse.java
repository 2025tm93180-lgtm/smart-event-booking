package org.afterglow.paymentservice.dto;

import java.math.BigDecimal;

public class CreateOrderResponse {
    private String order_id;
    private BigDecimal amount;
    private String currency;

    public CreateOrderResponse() {
    }

    public CreateOrderResponse(String order_id, BigDecimal amount, String currency) {
        this.order_id = order_id;
        this.amount = amount;
        this.currency = currency;
    }

    public String getOrder_id() {
        return order_id;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
