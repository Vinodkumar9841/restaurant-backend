package com.restaurant.restaurant_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurant.restaurant_backend.model.TableSession;

public interface TableSessionRepository extends JpaRepository<TableSession, Long> {
}