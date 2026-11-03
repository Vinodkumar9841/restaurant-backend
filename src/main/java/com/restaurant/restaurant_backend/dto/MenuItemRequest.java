package com.restaurant.restaurant_backend.dto;

import java.math.BigDecimal;

public class MenuItemRequest {
    private String name;
    private BigDecimal price;
    private String category;
    private Boolean available;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
}