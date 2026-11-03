package com.restaurant.restaurant_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurant.restaurant_backend.model.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
}