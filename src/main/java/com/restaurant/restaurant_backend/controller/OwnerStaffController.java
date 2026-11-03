package com.restaurant.restaurant_backend.controller;
 
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
 
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import com.restaurant.restaurant_backend.dto.CreateStaffRequest;
import com.restaurant.restaurant_backend.model.KitchenUser;
import com.restaurant.restaurant_backend.model.Order;
import com.restaurant.restaurant_backend.model.Restaurant;
import com.restaurant.restaurant_backend.repository.KitchenUserRepository;
import com.restaurant.restaurant_backend.repository.OrderRepository;
import com.restaurant.restaurant_backend.repository.RestaurantRepository;
import com.restaurant.restaurant_backend.service.ChefAssignmentService;
 
@RestController
@RequestMapping("/api/owner/staff")
@CrossOrigin(origins = "*")
public class OwnerStaffController {
 
    private final KitchenUserRepository kitchenUserRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;
    private final ChefAssignmentService chefAssignmentService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
 
    public OwnerStaffController(KitchenUserRepository kitchenUserRepository,
                                RestaurantRepository restaurantRepository,
                                OrderRepository orderRepository,
                                ChefAssignmentService chefAssignmentService) {
        this.kitchenUserRepository = kitchenUserRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderRepository = orderRepository;
        this.chefAssignmentService = chefAssignmentService;
    }
 
    private Long currentRestaurantId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
    }
 
    // One chef as the dashboard needs it: details plus how busy they are right now
    private Map<String, Object> toSummary(KitchenUser chef, List<Order> allOrders) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", chef.getId());
        summary.put("name", chef.getName());
        summary.put("offline", Boolean.TRUE.equals(chef.getOffline()));
        summary.put("maxConcurrentOrders", chef.getMaxConcurrentOrders() == null ? 3 : chef.getMaxConcurrentOrders());
        summary.put("activeOrders", chefAssignmentService.countActiveOrders(chef.getId(), allOrders));
        return summary;
    }
 
    private KitchenUser findOwnChef(Long id) {
        Long restaurantId = currentRestaurantId();
        KitchenUser chef = kitchenUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff member not found"));
        if (chef.getRestaurant() == null || !chef.getRestaurant().getId().equals(restaurantId)) {
            throw new RuntimeException("You do not have permission to change this staff member");
        }
        return chef;
    }
 
    @GetMapping
    public List<Map<String, Object>> getMyStaff() {
        Long restaurantId = currentRestaurantId();
        List<Order> allOrders = orderRepository.findAll();
        return kitchenUserRepository.findAll().stream()
                .filter(u -> u.getRestaurant() != null && u.getRestaurant().getId().equals(restaurantId))
                .map(u -> toSummary(u, allOrders))
                .collect(Collectors.toList());
    }
 
    @PostMapping
    public Map<String, Object> addStaff(@RequestBody CreateStaffRequest request) {
        if (request.getPin() == null || !request.getPin().matches("\\d{4}")) {
            throw new RuntimeException("PIN must be exactly 4 digits");
        }
 
        Long restaurantId = currentRestaurantId();
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
 
        KitchenUser user = new KitchenUser();
        user.setName(request.getName());
        user.setPinHash(passwordEncoder.encode(request.getPin()));
        user.setRestaurant(restaurant);
        KitchenUser saved = kitchenUserRepository.save(user);
 
        // A new chef can take over any orders that were waiting
        chefAssignmentService.assignWaitingOrders(restaurantId);
 
        return toSummary(saved, orderRepository.findAll());
    }
 
    // Body can contain "offline" (true/false) and/or "maxConcurrentOrders" (1 to 20)
    @PatchMapping("/{id}/availability")
    public Map<String, Object> updateAvailability(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        KitchenUser chef = findOwnChef(id);
 
        if (body.get("offline") != null) {
            chef.setOffline(Boolean.parseBoolean(String.valueOf(body.get("offline"))));
        }
        if (body.get("maxConcurrentOrders") != null) {
            int capacity = ((Number) body.get("maxConcurrentOrders")).intValue();
            if (capacity < 1 || capacity > 20) {
                throw new RuntimeException("Capacity must be between 1 and 20");
            }
            chef.setMaxConcurrentOrders(capacity);
        }
        KitchenUser saved = kitchenUserRepository.save(chef);
 
        // Room may have opened up (chef came online or capacity grew)
        chefAssignmentService.assignWaitingOrders(currentRestaurantId());
 
        return toSummary(saved, orderRepository.findAll());
    }
 
    @DeleteMapping("/{id}")
    public void removeStaff(@PathVariable Long id) {
        KitchenUser chef = findOwnChef(id);
        Long restaurantId = currentRestaurantId();
 
        // Orders assigned to this chef go back to waiting, so nothing points at a deleted chef
        for (Order order : orderRepository.findAll()) {
            if (order.getAssignedChef() != null && order.getAssignedChef().getId().equals(chef.getId())) {
                order.setAssignedChef(null);
                orderRepository.save(order);
            }
        }
 
        kitchenUserRepository.delete(chef);
 
        // Hand the orders that just lost their chef to the remaining chefs
        chefAssignmentService.assignWaitingOrders(restaurantId);
    }
}