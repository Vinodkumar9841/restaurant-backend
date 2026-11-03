package com.restaurant.restaurant_backend.controller;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.restaurant_backend.dto.CreateSessionRequest;
import com.restaurant.restaurant_backend.model.Restaurant;
import com.restaurant.restaurant_backend.model.TableSession;
import com.restaurant.restaurant_backend.repository.RestaurantRepository;
import com.restaurant.restaurant_backend.repository.TableSessionRepository;

@RestController
@RequestMapping("/api/session")
@CrossOrigin(origins = "*")
public class SessionController {

    private final TableSessionRepository tableSessionRepository;
    private final RestaurantRepository restaurantRepository;

    public SessionController(TableSessionRepository tableSessionRepository, RestaurantRepository restaurantRepository) {
        this.tableSessionRepository = tableSessionRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @PostMapping
    public TableSession createSession(@RequestBody CreateSessionRequest request) {
        Long restaurantId = request.getRestaurantId() != null ? request.getRestaurantId() : 1L;
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        TableSession session = new TableSession();
        session.setTableNumber(request.getTableNumber());
        session.setCustomerName(request.getCustomerName());
        session.setCreatedAt(LocalDateTime.now());
        session.setRestaurant(restaurant);
        return tableSessionRepository.save(session);
    }
}