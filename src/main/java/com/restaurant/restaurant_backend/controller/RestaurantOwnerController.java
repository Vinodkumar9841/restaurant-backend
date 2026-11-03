package com.restaurant.restaurant_backend.controller;

import java.time.LocalDateTime;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.restaurant_backend.dto.OwnerLoginRequest;
import com.restaurant.restaurant_backend.dto.OwnerLoginResponse;
import com.restaurant.restaurant_backend.dto.OwnerSignupRequest;
import com.restaurant.restaurant_backend.model.Restaurant;
import com.restaurant.restaurant_backend.model.RestaurantOwner;
import com.restaurant.restaurant_backend.repository.RestaurantOwnerRepository;
import com.restaurant.restaurant_backend.repository.RestaurantRepository;
import com.restaurant.restaurant_backend.security.JwtUtil;

@RestController
@RequestMapping("/api/owner")
@CrossOrigin(origins = "*")
public class RestaurantOwnerController {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantOwnerRepository restaurantOwnerRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public RestaurantOwnerController(RestaurantRepository restaurantRepository,
                                      RestaurantOwnerRepository restaurantOwnerRepository,
                                      JwtUtil jwtUtil) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantOwnerRepository = restaurantOwnerRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/signup")
    public OwnerLoginResponse signup(@RequestBody OwnerSignupRequest request) {
        if (restaurantOwnerRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("An account with this email already exists");
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.getRestaurantName());
        restaurant.setAddress(request.getAddress());
        restaurant.setCreatedAt(LocalDateTime.now());
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        RestaurantOwner owner = new RestaurantOwner();
        owner.setRestaurant(savedRestaurant);
        owner.setName(request.getOwnerName());
        owner.setEmail(request.getEmail());
        owner.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        owner.setCreatedAt(LocalDateTime.now());
        restaurantOwnerRepository.save(owner);

        String token = jwtUtil.generateToken(owner.getEmail(), savedRestaurant.getId(), "OWNER");
        return new OwnerLoginResponse(token, owner.getName(), savedRestaurant.getId(), savedRestaurant.getName());
    }

    @PostMapping("/login")
    public OwnerLoginResponse login(@RequestBody OwnerLoginRequest request) {
        RestaurantOwner owner = restaurantOwnerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), owner.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(owner.getEmail(), owner.getRestaurant().getId(), "OWNER");
        return new OwnerLoginResponse(token, owner.getName(), owner.getRestaurant().getId(), owner.getRestaurant().getName());
    }
}