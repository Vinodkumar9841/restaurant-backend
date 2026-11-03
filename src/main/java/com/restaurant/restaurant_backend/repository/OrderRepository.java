package com.restaurant.restaurant_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurant.restaurant_backend.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}