package com.restaurant.restaurant_backend.controller;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
 
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
 
import com.restaurant.restaurant_backend.model.MenuItem;
import com.restaurant.restaurant_backend.model.OrderItem;
import com.restaurant.restaurant_backend.repository.MenuItemRepository;
import com.restaurant.restaurant_backend.repository.OrderItemRepository;
 
@RestController
@RequestMapping("/api/public/restaurants")
@CrossOrigin(origins = "*")
public class RecommendationController {
 
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
 
    public RecommendationController(OrderItemRepository orderItemRepository,
                                    MenuItemRepository menuItemRepository) {
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
    }
 
    // popular  = this restaurant's best sellers
    // pairings = items other customers ordered together with what is in the cart now
    @GetMapping("/{restaurantId}/recommendations")
    public Map<String, Object> recommendations(@PathVariable Long restaurantId,
                                               @RequestParam(required = false) List<Long> cart) {
        Set<Long> inCart = cart == null ? new HashSet<>() : new HashSet<>(cart);
 
        // Only items that customers can see right now
        Map<Long, MenuItem> menu = new HashMap<>();
        for (MenuItem m : menuItemRepository.findAll()) {
            if (m.getRestaurant() != null && restaurantId.equals(m.getRestaurant().getId())
                    && !Boolean.FALSE.equals(m.getAvailable())) {
                menu.put(m.getId(), m);
            }
        }
 
        // Only this restaurant's order lines
        List<OrderItem> lines = orderItemRepository.findAll().stream()
                .filter(l -> l.getOrder() != null && l.getOrder().getRestaurant() != null
                        && restaurantId.equals(l.getOrder().getRestaurant().getId())
                        && l.getMenuItem() != null)
                .collect(Collectors.toList());
 
        Map<Long, Integer> popularity = new HashMap<>();
        Map<Long, Set<Long>> itemsByOrder = new HashMap<>();
        for (OrderItem line : lines) {
            Long itemId = line.getMenuItem().getId();
            Integer qty = line.getQuantity();
            popularity.merge(itemId, qty == null ? 1 : qty, Integer::sum);
            itemsByOrder.computeIfAbsent(line.getOrder().getId(), k -> new HashSet<>()).add(itemId);
        }
 
        // Count how often each item shows up in orders that also contain a cart item
        Map<Long, Integer> pairCounts = new HashMap<>();
        if (!inCart.isEmpty()) {
            for (Set<Long> orderItems : itemsByOrder.values()) {
                boolean hasCartItem = orderItems.stream().anyMatch(inCart::contains);
                if (!hasCartItem) continue;
                for (Long other : orderItems) {
                    if (!inCart.contains(other)) pairCounts.merge(other, 1, Integer::sum);
                }
            }
        }
 
        Map<String, Object> result = new HashMap<>();
        result.put("popular", topItems(popularity, menu, new HashSet<>()));
        result.put("pairings", topItems(pairCounts, menu, inCart));
        return result;
    }
 
    // Highest counts first, at most 3, skipping hidden or excluded items
    private List<Map<String, Object>> topItems(Map<Long, Integer> counts, Map<Long, MenuItem> menu, Set<Long> exclude) {
        List<Long> ids = counts.entrySet().stream()
                .filter(e -> menu.containsKey(e.getKey()) && !exclude.contains(e.getKey()))
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
 
        List<Map<String, Object>> out = new ArrayList<>();
        for (Long id : ids) {
            MenuItem m = menu.get(id);
            Map<String, Object> row = new HashMap<>();
            row.put("id", m.getId());
            row.put("name", m.getName());
            row.put("price", m.getPrice());
            row.put("category", m.getCategory());
            out.add(row);
        }
        return out;
    }
}