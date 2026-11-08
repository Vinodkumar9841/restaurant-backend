package com.restaurant.restaurant_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "restaurant_tables")
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @Column(name = "table_number")
    private Integer tableNumber;

    @Column(name = "is_occupied")
    private Boolean isOccupied;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Last time a guest picked this table or ordered from it (used to free forgotten tables)
    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    // true once the guest has placed an order (a confirmed guest keeps the table longer)
    @Column(name = "order_confirmed")
    private Boolean orderConfirmed = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Restaurant getRestaurant() { return restaurant; }
    public void setRestaurant(Restaurant restaurant) { this.restaurant = restaurant; }

    public Integer getTableNumber() { return tableNumber; }
    public void setTableNumber(Integer tableNumber) { this.tableNumber = tableNumber; }

    public Boolean getIsOccupied() { return isOccupied; }
    public void setIsOccupied(Boolean isOccupied) { this.isOccupied = isOccupied; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    public Boolean getOrderConfirmed() { return orderConfirmed; }
    public void setOrderConfirmed(Boolean orderConfirmed) { this.orderConfirmed = orderConfirmed; }
}
