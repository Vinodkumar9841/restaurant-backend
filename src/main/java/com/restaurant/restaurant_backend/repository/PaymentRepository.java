package com.restaurant.restaurant_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurant.restaurant_backend.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}