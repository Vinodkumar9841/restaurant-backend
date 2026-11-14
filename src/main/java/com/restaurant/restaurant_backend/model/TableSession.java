package com.restaurant.restaurant_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "table_sessions")
public class TableSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_number")
    private Integer tableNumber;

    @Column(name = "customer_name")
    private String customerName;

    // DINE_IN or TAKEAWAY
    @Column(name = "order_type")
    private String orderType = "DINE_IN";

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Table tab: the guest asked for the bill / the owner has taken payment
    @Column(name = "bill_requested")
    private Boolean billRequested = false;

    @Column(name = "bill_requested_at")
    private LocalDateTime billRequestedAt;

    // "Call waiter": the guest asked for help, the owner has not cleared it yet
    @Column(name = "waiter_called")
    private Boolean waiterCalled = false;

    @Column(name = "waiter_called_at")
    private LocalDateTime waiterCalledAt;

    @Column(name = "settled")
    private Boolean settled = false;

    @Column(name = "settled_method")
    private String settledMethod;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getTableNumber() { return tableNumber; }
    public void setTableNumber(Integer tableNumber) { this.tableNumber = tableNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getBillRequested() { return billRequested; }
    public void setBillRequested(Boolean billRequested) { this.billRequested = billRequested; }

    public LocalDateTime getBillRequestedAt() { return billRequestedAt; }
    public void setBillRequestedAt(LocalDateTime billRequestedAt) { this.billRequestedAt = billRequestedAt; }

    public Boolean getWaiterCalled() { return waiterCalled; }
    public void setWaiterCalled(Boolean waiterCalled) { this.waiterCalled = waiterCalled; }

    public LocalDateTime getWaiterCalledAt() { return waiterCalledAt; }
    public void setWaiterCalledAt(LocalDateTime waiterCalledAt) { this.waiterCalledAt = waiterCalledAt; }

    public Boolean getSettled() { return settled; }
    public void setSettled(Boolean settled) { this.settled = settled; }

    public String getSettledMethod() { return settledMethod; }
    public void setSettledMethod(String settledMethod) { this.settledMethod = settledMethod; }

    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }

    public Restaurant getRestaurant() { return restaurant; }
    public void setRestaurant(Restaurant restaurant) { this.restaurant = restaurant; }
}
