package com.restaurant.restaurant_backend.dto;

public class LoginRequest {
    private String pin;
    private Long restaurantId;

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }
}