package com.restaurant.restaurant_backend.dto;

import java.math.BigDecimal;

public class CreatePaymentRequest {
    private Long tableSessionId;
    private BigDecimal amount;

    public Long getTableSessionId() { return tableSessionId; }
    public void setTableSessionId(Long tableSessionId) { this.tableSessionId = tableSessionId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}