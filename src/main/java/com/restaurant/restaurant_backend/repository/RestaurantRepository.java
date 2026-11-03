package com.restaurant.restaurant_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurant.restaurant_backend.model.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
}