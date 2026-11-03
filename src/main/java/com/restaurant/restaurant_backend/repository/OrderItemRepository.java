package com.restaurant.restaurant_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurant.restaurant_backend.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}