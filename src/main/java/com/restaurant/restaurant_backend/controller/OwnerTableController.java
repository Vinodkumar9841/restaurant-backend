package com.restaurant.restaurant_backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.restaurant_backend.dto.CreateTableRequest;
import com.restaurant.restaurant_backend.model.Restaurant;
import com.restaurant.restaurant_backend.model.RestaurantTable;
import com.restaurant.restaurant_backend.repository.RestaurantRepository;
import com.restaurant.restaurant_backend.repository.RestaurantTableRepository;

@RestController
@RequestMapping("/api/owner/tables")
@CrossOrigin(origins = "*")
public class OwnerTableController {

    private final RestaurantTableRepository tableRepository;
    private final RestaurantRepository restaurantRepository;

    public OwnerTableController(RestaurantTableRepository tableRepository, RestaurantRepository restaurantRepository) {
        this.tableRepository = tableRepository;
        this.restaurantRepository = restaurantRepository;
    }

    private Long currentRestaurantId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
    }

    @GetMapping
    public List<RestaurantTable> getMyTables() {
        Long restaurantId = currentRestaurantId();
        return tableRepository.findAll().stream()
                .filter(t -> t.getRestaurant() != null && t.getRestaurant().getId().equals(restaurantId))
                .collect(Collectors.toList());
    }

    @PostMapping
    public RestaurantTable addTable(@RequestBody CreateTableRequest request) {
        Long restaurantId = currentRestaurantId();
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        RestaurantTable table = new RestaurantTable();
        table.setRestaurant(restaurant);
        table.setTableNumber(request.getTableNumber());
        table.setIsOccupied(false);
        table.setCreatedAt(LocalDateTime.now());

        return tableRepository.save(table);
    }
}