package com.restaurant.restaurant_backend.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.restaurant_backend.model.Order;
import com.restaurant.restaurant_backend.model.OrderItem;
import com.restaurant.restaurant_backend.repository.OrderItemRepository;
import com.restaurant.restaurant_backend.repository.OrderRepository;

@RestController
@CrossOrigin(origins = "*")
public class OwnerBillController {

    // Menu prices already include GST (5% = 2.5% CGST + 2.5% SGST), so the bill total
    // always equals the amount the guest actually paid.
    private static final BigDecimal GST_DIVISOR = new BigDecimal("1.05");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OwnerBillController(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @GetMapping("/api/owner/orders/{id}/bill")
    public Map<String, Object> bill(@PathVariable Long id) {
        Long restaurantId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getRestaurant() == null || !order.getRestaurant().getId().equals(restaurantId)) {
            throw new RuntimeException("You do not have permission to view this bill");
        }

        List<Map<String, Object>> lines = new ArrayList<>();
        for (OrderItem oi : orderItemRepository.findAll()) {
            if (oi.getOrder() == null || !id.equals(oi.getOrder().getId())) continue;
            BigDecimal price = oi.getPriceAtOrder() != null ? oi.getPriceAtOrder() : BigDecimal.ZERO;
            int qty = oi.getQuantity() != null ? oi.getQuantity() : 0;
            Map<String, Object> line = new HashMap<>();
            line.put("name", oi.getMenuItem() != null ? oi.getMenuItem().getName() : "Item");
            line.put("quantity", qty);
            line.put("price", price);
            line.put("lineTotal", price.multiply(BigDecimal.valueOf(qty)));
            lines.add(line);
        }

        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal taxable = total.divide(GST_DIVISOR, 2, RoundingMode.HALF_UP);
        BigDecimal gst = total.subtract(taxable);
        BigDecimal cgst = gst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal sgst = gst.subtract(cgst);

        String payStatus = order.getPayment() != null ? order.getPayment().getStatus() : null;

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("restaurantName", order.getRestaurant().getName());
        result.put("restaurantAddress", order.getRestaurant().getAddress());
        result.put("createdAt", order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);
        result.put("customerName", order.getTableSession() != null ? order.getTableSession().getCustomerName() : null);
        result.put("orderType", order.getTableSession() != null ? order.getTableSession().getOrderType() : null);
        result.put("tableNumber", order.getTableSession() != null ? order.getTableSession().getTableNumber() : null);
        result.put("tokenNumber", order.getTokenNumber());
        result.put("paymentMethod", "PAID".equals(payStatus) ? "Online" : "Cash");
        result.put("items", lines);
        result.put("taxableValue", taxable);
        result.put("cgst", cgst);
        result.put("sgst", sgst);
        result.put("total", total);
        return result;
    }
}
