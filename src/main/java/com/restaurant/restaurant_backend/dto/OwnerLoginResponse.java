package com.restaurant.restaurant_backend.dto;

public class OwnerLoginResponse {
    private String token;
    private String name;
    private Long restaurantId;
    private String restaurantName;

    public OwnerLoginResponse(String token, String name, Long restaurantId, String restaurantName) {
        this.token = token;
        this.name = name;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }
}