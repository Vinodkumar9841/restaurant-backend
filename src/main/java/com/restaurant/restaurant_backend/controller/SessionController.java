package com.restaurant.restaurant_backend.controller;

import com.restaurant.restaurant_backend.dto.CreateSessionRequest;
import com.restaurant.restaurant_backend.model.Restaurant;
import com.restaurant.restaurant_backend.model.TableSession;
import com.restaurant.restaurant_backend.repository.RestaurantRepository;
import com.restaurant.restaurant_backend.repository.TableSessionRepository;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

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

        boolean takeaway = "TAKEAWAY".equals(request.getOrderType());

        TableSession session = new TableSession();
        session.setOrderType(takeaway ? "TAKEAWAY" : "DINE_IN");
        // Takeaway guests have no table
        session.setTableNumber(takeaway ? null : request.getTableNumber());
        session.setCustomerName(request.getCustomerName());
        session.setCustomerPhone(request.getCustomerPhone());
        session.setCreatedAt(LocalDateTime.now());
        session.setRestaurant(restaurant);
        return tableSessionRepository.save(session);
    }
}
