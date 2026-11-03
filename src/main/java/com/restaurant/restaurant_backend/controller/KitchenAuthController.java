package com.restaurant.restaurant_backend.controller;

import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.restaurant_backend.dto.LoginRequest;
import com.restaurant.restaurant_backend.dto.LoginResponse;
import com.restaurant.restaurant_backend.model.KitchenUser;
import com.restaurant.restaurant_backend.repository.KitchenUserRepository;
import com.restaurant.restaurant_backend.security.JwtUtil;

@RestController
@RequestMapping("/api/kitchen")
@CrossOrigin(origins = "*")
public class KitchenAuthController {

    private final KitchenUserRepository kitchenUserRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public KitchenAuthController(KitchenUserRepository kitchenUserRepository, JwtUtil jwtUtil) {
        this.kitchenUserRepository = kitchenUserRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        Long restaurantId = request.getRestaurantId() != null ? request.getRestaurantId() : 1L;

        List<KitchenUser> restaurantStaff = kitchenUserRepository.findAll().stream()
                .filter(user -> user.getRestaurant() != null
                        && user.getRestaurant().getId().equals(restaurantId))
                .toList();

        for (KitchenUser user : restaurantStaff) {
            if (passwordEncoder.matches(request.getPin(), user.getPinHash())) {

                String token = jwtUtil.generateToken(
                        user.getName(),
                        restaurantId,
                        "KITCHEN"
                );

                return new LoginResponse(token, user.getName());
            }
        }

        throw new RuntimeException("Invalid PIN");
    }
}