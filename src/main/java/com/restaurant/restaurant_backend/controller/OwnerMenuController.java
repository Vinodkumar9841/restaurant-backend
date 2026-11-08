package com.restaurant.restaurant_backend.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.restaurant_backend.dto.MenuItemRequest;
import com.restaurant.restaurant_backend.model.MenuItem;
import com.restaurant.restaurant_backend.model.Restaurant;
import com.restaurant.restaurant_backend.repository.MenuItemRepository;
import com.restaurant.restaurant_backend.repository.RestaurantRepository;

@RestController
@RequestMapping("/api/owner/menu")
@CrossOrigin(origins = "*")
public class OwnerMenuController {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    public OwnerMenuController(MenuItemRepository menuItemRepository, RestaurantRepository restaurantRepository) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
    }

    private Long currentRestaurantId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
    }

    @GetMapping
    public List<MenuItem> getMyMenu() {
        Long restaurantId = currentRestaurantId();
        return menuItemRepository.findAll().stream()
                .filter(item -> item.getRestaurant() != null && item.getRestaurant().getId().equals(restaurantId))
                .collect(Collectors.toList());
    }

    @PostMapping
    public MenuItem addItem(@RequestBody MenuItemRequest request) {
        Long restaurantId = currentRestaurantId();
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        MenuItem item = new MenuItem();
        item.setName(request.getName());
        item.setPrice(request.getPrice());
        item.setCategory(request.getCategory());
        item.setAvailable(request.getAvailable() != null ? request.getAvailable() : true);
        item.setImageUrl(request.getImageUrl());
        item.setIsVeg(request.getIsVeg());
        item.setRestaurant(restaurant);

        return menuItemRepository.save(item);
    }

    @PutMapping("/{id}")
    public MenuItem updateItem(@PathVariable Long id, @RequestBody MenuItemRequest request) {
        Long restaurantId = currentRestaurantId();
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));

        if (item.getRestaurant() == null || !item.getRestaurant().getId().equals(restaurantId)) {
            throw new RuntimeException("You do not have permission to edit this item");
        }

        if (request.getName() != null) item.setName(request.getName());
        if (request.getPrice() != null) item.setPrice(request.getPrice());
        if (request.getCategory() != null) item.setCategory(request.getCategory());
        if (request.getAvailable() != null) item.setAvailable(request.getAvailable());
        if (request.getImageUrl() != null) item.setImageUrl(request.getImageUrl().trim().isEmpty() ? null : request.getImageUrl().trim());
        if (request.getIsVeg() != null) item.setIsVeg(request.getIsVeg());

        return menuItemRepository.save(item);
    }

    @DeleteMapping("/{id}")
    public MenuItem markUnavailable(@PathVariable Long id) {
        Long restaurantId = currentRestaurantId();
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));

        if (item.getRestaurant() == null || !item.getRestaurant().getId().equals(restaurantId)) {
            throw new RuntimeException("You do not have permission to remove this item");
        }

        item.setAvailable(false);
        return menuItemRepository.save(item);
    }
}