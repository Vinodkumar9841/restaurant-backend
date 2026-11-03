package com.restaurant.restaurant_backend.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.restaurant_backend.model.Order;
import com.restaurant.restaurant_backend.repository.OrderRepository;

@RestController
@RequestMapping("/api/owner/dashboard")
@CrossOrigin(origins = "*")
public class OwnerDashboardController {

    private final OrderRepository orderRepository;

    public OwnerDashboardController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        Long restaurantId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();

        List<Order> restaurantOrders = orderRepository.findAll().stream()
                .filter(order -> order.getRestaurant() != null && order.getRestaurant().getId().equals(restaurantId))
                .collect(Collectors.toList());

        BigDecimal totalRevenue = restaurantOrders.stream()
                .map(Order::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingCount = restaurantOrders.stream().filter(o -> "PENDING".equals(o.getStatus())).count();
        long preparingCount = restaurantOrders.stream().filter(o -> "PREPARING".equals(o.getStatus())).count();
        long servedCount = restaurantOrders.stream().filter(o -> "SERVED".equals(o.getStatus())).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalOrders", restaurantOrders.size());
        summary.put("totalRevenue", totalRevenue);
        summary.put("pendingCount", pendingCount);
        summary.put("preparingCount", preparingCount);
        summary.put("servedCount", servedCount);

        return summary;
    }

    @GetMapping("/orders")
    public List<Order> getOrders() {
        Long restaurantId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();

        return orderRepository.findAll().stream()
                .filter(order -> order.getRestaurant() != null && order.getRestaurant().getId().equals(restaurantId))
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .collect(Collectors.toList());
    }
}