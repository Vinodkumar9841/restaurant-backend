package com.restaurant.restaurant_backend.service;
 
import java.util.Comparator;
import java.util.List;
 
import org.springframework.stereotype.Service;
 
import com.restaurant.restaurant_backend.model.KitchenUser;
import com.restaurant.restaurant_backend.model.Order;
import com.restaurant.restaurant_backend.repository.KitchenUserRepository;
import com.restaurant.restaurant_backend.repository.OrderRepository;
 
@Service
public class ChefAssignmentService {
 
    private final KitchenUserRepository kitchenUserRepository;
    private final OrderRepository orderRepository;
 
    public ChefAssignmentService(KitchenUserRepository kitchenUserRepository, OrderRepository orderRepository) {
        this.kitchenUserRepository = kitchenUserRepository;
        this.orderRepository = orderRepository;
    }
 
    // How many orders this chef is working on right now (PENDING or PREPARING)
    public long countActiveOrders(Long chefId, List<Order> allOrders) {
        return allOrders.stream()
                .filter(o -> o.getAssignedChef() != null
                        && o.getAssignedChef().getId().equals(chefId)
                        && ("PENDING".equals(o.getStatus()) || "PREPARING".equals(o.getStatus())))
                .count();
    }
 
    // Picks the best chef for a new order in this restaurant.
    // Returns null if every chef is offline or full (the order then waits unassigned).
    public KitchenUser pickChef(Long restaurantId) {
        List<Order> allOrders = orderRepository.findAll();
 
        KitchenUser bestChef = null;
        double bestRatio = Double.MAX_VALUE;
 
        for (KitchenUser chef : kitchenUserRepository.findAll()) {
            if (chef.getRestaurant() == null || !chef.getRestaurant().getId().equals(restaurantId)) {
                continue; // a chef from another restaurant
            }
            if (Boolean.TRUE.equals(chef.getOffline())) {
                continue; // offline chefs get nothing
            }
 
            int capacity = chef.getMaxConcurrentOrders() == null ? 3 : chef.getMaxConcurrentOrders();
            long activeOrders = countActiveOrders(chef.getId(), allOrders);
 
            if (activeOrders >= capacity) {
                continue; // this chef is full
            }
 
            double ratio = (double) activeOrders / capacity; // 0.0 = idle, 1.0 = full
            if (ratio < bestRatio) {
                bestRatio = ratio;
                bestChef = chef;
            }
        }
 
        return bestChef;
    }
 
    // Hands out orders that were waiting (oldest first) while there is a chef with room.
    // Called whenever room may have opened up: an order is served, a chef comes online,
    // capacity changes, or a new chef is added.
    public void assignWaitingOrders(Long restaurantId) {
        List<Order> waiting = orderRepository.findAll().stream()
                .filter(o -> o.getRestaurant() != null
                        && o.getRestaurant().getId().equals(restaurantId)
                        && o.getAssignedChef() == null
                        && "PENDING".equals(o.getStatus()))
                .sorted(Comparator.comparing(Order::getId))
                .toList();
 
        for (Order order : waiting) {
            KitchenUser chef = pickChef(restaurantId);
            if (chef == null) {
                break; // nobody has room right now
            }
            order.setAssignedChef(chef);
            orderRepository.save(order);
        }
    }
 
    // Finds a chef in this restaurant by name (the kitchen login token carries the name)
    public KitchenUser findChef(Long restaurantId, String name) {
        return kitchenUserRepository.findAll().stream()
                .filter(c -> c.getRestaurant() != null
                        && c.getRestaurant().getId().equals(restaurantId)
                        && c.getName() != null
                        && c.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}