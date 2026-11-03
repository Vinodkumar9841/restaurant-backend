package com.restaurant.restaurant_backend.repository;

import com.restaurant.restaurant_backend.model.KitchenUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KitchenUserRepository extends JpaRepository<KitchenUser, Long> {
    List<KitchenUser> findAll();
}