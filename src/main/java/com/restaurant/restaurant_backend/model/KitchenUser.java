package com.restaurant.restaurant_backend.model;
 
import com.fasterxml.jackson.annotation.JsonIgnore;
 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
 
@Entity
@Table(name = "kitchen_users")
public class KitchenUser {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    private String name;
 
    // @JsonIgnore keeps the PIN hash out of every API response
    @JsonIgnore
    @Column(name = "pin_hash")
    private String pinHash;
 
    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;
 
    // How many orders this chef can handle at the same time
    @Column(name = "max_concurrent_orders")
    private Integer maxConcurrentOrders = 3;
 
    // When true, this chef gets no new orders (on break, gone home, etc.)
    @Column(name = "is_offline")
    private Boolean offline = false;
 
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
 
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
 
    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
 
    public Restaurant getRestaurant() { return restaurant; }
    public void setRestaurant(Restaurant restaurant) { this.restaurant = restaurant; }
 
    public Integer getMaxConcurrentOrders() { return maxConcurrentOrders; }
    public void setMaxConcurrentOrders(Integer maxConcurrentOrders) { this.maxConcurrentOrders = maxConcurrentOrders; }
 
    public Boolean getOffline() { return offline; }
    public void setOffline(Boolean offline) { this.offline = offline; }
}