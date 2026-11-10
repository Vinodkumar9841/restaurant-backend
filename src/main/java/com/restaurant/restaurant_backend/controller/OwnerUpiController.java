package com.restaurant.restaurant_backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.restaurant_backend.model.Restaurant;
import com.restaurant.restaurant_backend.repository.RestaurantRepository;

@RestController
@CrossOrigin(origins = "*")
public class OwnerUpiController {

    private final RestaurantRepository restaurantRepository;

    public OwnerUpiController(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    private Restaurant myRestaurant() {
        Long restaurantId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
    }

    @GetMapping("/api/owner/upi")
    public Map<String, Object> getUpi() {
        Map<String, Object> result = new HashMap<>();
        String upi = myRestaurant().getUpiId();
        result.put("upiId", upi == null ? "" : upi);
        return result;
    }

    @PutMapping("/api/owner/upi")
    public Map<String, Object> saveUpi(@RequestBody Map<String, Object> body) {
        String upi = String.valueOf(body.getOrDefault("upiId", "")).trim();
        if (!upi.isEmpty() && !upi.matches("^[a-zA-Z0-9._-]{2,}@[a-zA-Z]{2,}$")) {
            throw new RuntimeException("That does not look like a UPI ID (example: shopname@okbank)");
        }
        Restaurant r = myRestaurant();
        r.setUpiId(upi.isEmpty() ? null : upi);
        restaurantRepository.save(r);
        Map<String, Object> result = new HashMap<>();
        result.put("upiId", upi);
        return result;
    }
}
