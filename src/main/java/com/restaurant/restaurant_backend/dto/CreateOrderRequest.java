package com.restaurant.restaurant_backend.dto;

import java.util.List;

public class CreateOrderRequest {
    private Long tableSessionId;
    private Long paymentId;
    private List<OrderItemRequest> items;

    public Long getTableSessionId() { return tableSessionId; }
    public void setTableSessionId(Long tableSessionId) { this.tableSessionId = tableSessionId; }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
}