package com.restaurant.restaurant_backend.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.restaurant_backend.model.Order;
import com.restaurant.restaurant_backend.model.OrderItem;
import com.restaurant.restaurant_backend.repository.OrderItemRepository;
import com.restaurant.restaurant_backend.repository.OrderRepository;

// Sales numbers for the owner dashboard. Only this restaurant's orders are counted.
@RestController
@CrossOrigin(origins = "*")
public class OwnerAnalyticsController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OwnerAnalyticsController(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    private static BigDecimal money(Order o) {
        return o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
    }

    // CASH, UPI or ONLINE. For "pay after meal" orders, how the table was actually settled.
    private static String payLabel(Order o) {
        String mode = o.getPaymentMode();
        if ("PAY_LATER".equals(mode)) {
            var s = o.getTableSession();
            if (s != null && Boolean.TRUE.equals(s.getSettled()) && s.getSettledMethod() != null) {
                return s.getSettledMethod();
            }
            return "UNPAID";
        }
        if ("UPI".equals(mode)) return "UPI";
        if ("ONLINE".equals(mode)) return "ONLINE";
        if (mode == null && o.getPayment() != null && "PAID".equals(o.getPayment().getStatus())) return "ONLINE";
        return "CASH";
    }

    @GetMapping("/api/owner/analytics")
    public Map<String, Object> analytics() {
        Long restaurantId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        LocalDate monthStart = today.minusDays(29);

        List<Order> mine = new ArrayList<>();
        for (Order o : orderRepository.findAll()) {
            if (o.getRestaurant() != null && restaurantId.equals(o.getRestaurant().getId()) && o.getCreatedAt() != null) {
                mine.add(o);
            }
        }

        // Last 7 days, oldest first (days with no orders show 0)
        Map<LocalDate, BigDecimal> daySales = new LinkedHashMap<>();
        Map<LocalDate, Integer> dayOrders = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            daySales.put(d, BigDecimal.ZERO);
            dayOrders.put(d, 0);
        }

        BigDecimal todaySales = BigDecimal.ZERO;
        int todayOrders = 0;
        int dineIn = 0;
        int takeaway = 0;
        Map<String, BigDecimal> byPayment = new HashMap<>();
        Map<Long, Boolean> monthOrderIds = new HashMap<>();

        for (Order o : mine) {
            LocalDate d = o.getCreatedAt().toLocalDate();
            if (daySales.containsKey(d)) {
                daySales.put(d, daySales.get(d).add(money(o)));
                dayOrders.put(d, dayOrders.get(d) + 1);
            }
            if (d.equals(today)) {
                todaySales = todaySales.add(money(o));
                todayOrders++;
            }
            if (!d.isBefore(weekStart)) {
                boolean take = o.getTableSession() != null && "TAKEAWAY".equals(o.getTableSession().getOrderType());
                if (take) takeaway++; else dineIn++;
                byPayment.merge(payLabel(o), money(o), BigDecimal::add);
            }
            if (!d.isBefore(monthStart)) {
                monthOrderIds.put(o.getId(), true);
            }
        }

        // Best sellers over the last 30 days
        Map<String, int[]> qty = new HashMap<>();
        Map<String, BigDecimal> revenue = new HashMap<>();
        for (OrderItem oi : orderItemRepository.findAll()) {
            if (oi.getOrder() == null || !monthOrderIds.containsKey(oi.getOrder().getId())) continue;
            String name = oi.getMenuItem() != null ? oi.getMenuItem().getName() : "Item";
            int q = oi.getQuantity() != null ? oi.getQuantity() : 0;
            BigDecimal price = oi.getPriceAtOrder() != null ? oi.getPriceAtOrder() : BigDecimal.ZERO;
            qty.computeIfAbsent(name, k -> new int[1])[0] += q;
            revenue.merge(name, price.multiply(BigDecimal.valueOf(q)), BigDecimal::add);
        }
        List<Map<String, Object>> top = new ArrayList<>();
        for (Map.Entry<String, int[]> e : qty.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", e.getKey());
            row.put("quantity", e.getValue()[0]);
            row.put("revenue", revenue.get(e.getKey()));
            top.add(row);
        }
        top.sort(Comparator.comparing((Map<String, Object> r) -> (Integer) r.get("quantity")).reversed());
        if (top.size() > 5) top = new ArrayList<>(top.subList(0, 5));

        List<Map<String, Object>> days = new ArrayList<>();
        BigDecimal weekSales = BigDecimal.ZERO;
        for (LocalDate d : daySales.keySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("date", d.toString());
            row.put("sales", daySales.get(d));
            row.put("orders", dayOrders.get(d));
            days.add(row);
            weekSales = weekSales.add(daySales.get(d));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("todaySales", todaySales);
        result.put("todayOrders", todayOrders);
        result.put("weekSales", weekSales);
        result.put("days", days);
        result.put("topItems", top);
        result.put("dineInOrders", dineIn);
        result.put("takeawayOrders", takeaway);
        result.put("byPayment", byPayment);
        return result;
    }
}
