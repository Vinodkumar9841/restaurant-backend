package com.restaurant.restaurant_backend.controller;
 
import java.util.HashMap;
import java.util.Map;
 
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import com.restaurant.restaurant_backend.model.Restaurant;
import com.restaurant.restaurant_backend.repository.RestaurantRepository;
 
@RestController
@RequestMapping("/api/public/restaurants")
@CrossOrigin(origins = "*")
public class PublicRestaurantController {
 
    private final RestaurantRepository restaurantRepository;
 
    public PublicRestaurantController(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }
 
    // Anyone scanning a QR code can see the restaurant's name and address (nothing private).
    @GetMapping("/{id}")
    public Map<String, Object> getRestaurant(@PathVariable Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
 
        Map<String, Object> result = new HashMap<>();
        result.put("id", restaurant.getId());
        result.put("name", restaurant.getName());
        result.put("address", restaurant.getAddress() == null ? "" : restaurant.getAddress());
        return result;
    }
}