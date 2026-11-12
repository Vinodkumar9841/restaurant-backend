package com.restaurant.restaurant_backend.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.restaurant_backend.model.Order;
import com.restaurant.restaurant_backend.model.OrderItem;
import com.restaurant.restaurant_backend.model.RestaurantTable;
import com.restaurant.restaurant_backend.model.TableSession;
import com.restaurant.restaurant_backend.repository.OrderItemRepository;
import com.restaurant.restaurant_backend.repository.OrderRepository;
import com.restaurant.restaurant_backend.repository.RestaurantTableRepository;
import com.restaurant.restaurant_backend.repository.TableSessionRepository;

// The "table tab": everything one guest ordered during a visit, the bill request, and settling up.
@RestController
@CrossOrigin(origins = "*")
public class TabController {

    private final TableSessionRepository sessionRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestaurantTableRepository tableRepository;

    public TabController(TableSessionRepository sessionRepository,
                         OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         RestaurantTableRepository tableRepository) {
        this.sessionRepository = sessionRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.tableRepository = tableRepository;
    }

    private List<Order> ordersOf(Long sessionId) {
        List<Order> list = new ArrayList<>();
        for (Order o : orderRepository.findAll()) {
            if (o.getTableSession() != null && sessionId.equals(o.getTableSession().getId())) list.add(o);
        }
        list.sort(Comparator.comparing(Order::getId));
        return list;
    }

    private BigDecimal totalOf(List<Order> orders) {
        BigDecimal total = BigDecimal.ZERO;
        for (Order o : orders) {
            if (o.getTotalAmount() != null) total = total.add(o.getTotalAmount());
        }
        return total;
    }

    // What the guest sees on "My Tab"
    @GetMapping("/api/public/sessions/{sessionId}/tab")
    public Map<String, Object> tab(@PathVariable Long sessionId) {
        TableSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        List<Order> orders = ordersOf(sessionId);
        List<OrderItem> allItems = orderItemRepository.findAll();

        List<Map<String, Object>> orderRows = new ArrayList<>();
        for (Order o : orders) {
            List<Map<String, Object>> lines = new ArrayList<>();
            for (OrderItem oi : allItems) {
                if (oi.getOrder() != null && o.getId().equals(oi.getOrder().getId())) {
                    Map<String, Object> line = new HashMap<>();
                    line.put("name", oi.getMenuItem() != null ? oi.getMenuItem().getName() : "Item");
                    line.put("quantity", oi.getQuantity());
                    BigDecimal price = oi.getPriceAtOrder() != null ? oi.getPriceAtOrder() : BigDecimal.ZERO;
                    line.put("lineTotal", price.multiply(BigDecimal.valueOf(oi.getQuantity() == null ? 0 : oi.getQuantity())));
                    lines.add(line);
                }
            }
            Map<String, Object> row = new HashMap<>();
            row.put("id", o.getId());
            row.put("status", o.getStatus());
            row.put("total", o.getTotalAmount());
            row.put("items", lines);
            orderRows.add(row);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("tableNumber", session.getTableNumber());
        result.put("customerName", session.getCustomerName());
        result.put("orders", orderRows);
        result.put("total", totalOf(orders));
        result.put("billRequested", Boolean.TRUE.equals(session.getBillRequested()));
        result.put("settled", Boolean.TRUE.equals(session.getSettled()));
        result.put("settledMethod", session.getSettledMethod());
        return result;
    }

    // The guest taps "Request bill"
    @PostMapping("/api/public/sessions/{sessionId}/request-bill")
    public Map<String, Object> requestBill(@PathVariable Long sessionId) {
        TableSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!Boolean.TRUE.equals(session.getBillRequested())) {
            session.setBillRequested(true);
            session.setBillRequestedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("billRequested", true);
        return result;
    }

    // The owner sees every table that is waiting to pay
    @GetMapping("/api/owner/bills")
    public List<Map<String, Object>> openBills() {
        Long restaurantId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TableSession s : sessionRepository.findAll()) {
            if (s.getRestaurant() == null || !restaurantId.equals(s.getRestaurant().getId())) continue;
            if (!Boolean.TRUE.equals(s.getBillRequested()) || Boolean.TRUE.equals(s.getSettled())) continue;
            List<Order> orders = ordersOf(s.getId());
            Map<String, Object> row = new HashMap<>();
            row.put("sessionId", s.getId());
            row.put("tableNumber", s.getTableNumber());
            row.put("customerName", s.getCustomerName());
            row.put("orderCount", orders.size());
            row.put("total", totalOf(orders));
            row.put("requestedAt", s.getBillRequestedAt() != null ? s.getBillRequestedAt().toString() : null);
            rows.add(row);
        }
        rows.sort(Comparator.comparing((Map<String, Object> r) -> String.valueOf(r.get("requestedAt"))));
        return rows;
    }

    // The owner took the payment: close the tab and free the table
    @PostMapping("/api/owner/sessions/{sessionId}/settle")
    public Map<String, Object> settle(@PathVariable Long sessionId, @RequestBody Map<String, Object> body) {
        Long restaurantId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        TableSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (session.getRestaurant() == null || !restaurantId.equals(session.getRestaurant().getId())) {
            throw new RuntimeException("You do not have permission to settle this bill");
        }
        String method = String.valueOf(body.getOrDefault("method", "CASH")).toUpperCase();
        if (!method.equals("UPI") && !method.equals("CASH")) method = "CASH";

        session.setSettled(true);
        session.setSettledMethod(method);
        session.setSettledAt(LocalDateTime.now());
        sessionRepository.save(session);

        if (session.getTableNumber() != null) {
            for (RestaurantTable t : tableRepository.findAll()) {
                if (t.getRestaurant() != null && restaurantId.equals(t.getRestaurant().getId())
                        && session.getTableNumber().equals(t.getTableNumber())) {
                    t.setIsOccupied(false);
                    t.setOrderConfirmed(false);
                    tableRepository.save(t);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("settled", true);
        result.put("method", method);
        return result;
    }
}
