package com.restaurant.restaurant_backend.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.restaurant_backend.model.MenuItem;
import com.restaurant.restaurant_backend.repository.MenuItemRepository;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "*")
public class MenuController {

    private final MenuItemRepository menuItemRepository;

    public MenuController(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @GetMapping
    public List<MenuItem> getMenu(@RequestParam(defaultValue = "1") Long restaurantId) {
        return menuItemRepository.findAll().stream()
                .filter(item -> item.getRestaurant() != null && item.getRestaurant().getId().equals(restaurantId))
                .collect(Collectors.toList());
    }
}