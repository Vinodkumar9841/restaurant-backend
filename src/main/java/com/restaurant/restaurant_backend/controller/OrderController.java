package com.restaurant.restaurant_backend.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.restaurant_backend.dto.CreateOrderRequest;
import com.restaurant.restaurant_backend.dto.OrderItemRequest;
import com.restaurant.restaurant_backend.dto.UpdateStatusRequest;
import com.restaurant.restaurant_backend.model.KitchenUser;
import com.restaurant.restaurant_backend.model.MenuItem;
import com.restaurant.restaurant_backend.model.Order;
import com.restaurant.restaurant_backend.model.OrderItem;
import com.restaurant.restaurant_backend.model.Payment;
import com.restaurant.restaurant_backend.model.TableSession;
import com.restaurant.restaurant_backend.repository.MenuItemRepository;
import com.restaurant.restaurant_backend.repository.OrderItemRepository;
import com.restaurant.restaurant_backend.repository.OrderRepository;
import com.restaurant.restaurant_backend.repository.PaymentRepository;
import com.restaurant.restaurant_backend.repository.RestaurantTableRepository;
import com.restaurant.restaurant_backend.repository.TableSessionRepository;
import com.restaurant.restaurant_backend.service.ChefAssignmentService;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final TableSessionRepository tableSessionRepository;
    private final PaymentRepository paymentRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChefAssignmentService chefAssignmentService;
    private final RestaurantTableRepository tableRepository;

    public OrderController(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            MenuItemRepository menuItemRepository,
                            TableSessionRepository tableSessionRepository,
                            PaymentRepository paymentRepository,
                            SimpMessagingTemplate messagingTemplate,
                            ChefAssignmentService chefAssignmentService,
                            RestaurantTableRepository tableRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.tableSessionRepository = tableSessionRepository;
        this.paymentRepository = paymentRepository;
        this.messagingTemplate = messagingTemplate;
        this.chefAssignmentService = chefAssignmentService;
        this.tableRepository = tableRepository;
    }

    @PostMapping
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        TableSession session = tableSessionRepository.findById(request.getTableSessionId())
                .orElseThrow(() -> new RuntimeException("Table session not found"));

        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        boolean validPayment = "PAID".equals(payment.getStatus()) || "CASH_PENDING".equals(payment.getStatus());
        if (!validPayment) {
            throw new RuntimeException("Payment not completed — cannot place order");
        }

        Order order = new Order();
        order.setTableSession(session);
        order.setPayment(payment);
        order.setRestaurant(session.getRestaurant());
        // Smart assignment: the least-busy online chef gets the whole order (null = everyone busy/offline)
        order.setAssignedChef(chefAssignmentService.pickChef(session.getRestaurant().getId()));
        order.setStatus("PENDING");
        order.setPaymentMode(request.getPaymentMode());
        order.setCreatedAt(LocalDateTime.now());
        if ("TAKEAWAY".equals(session.getOrderType())) {
            order.setTokenNumber(nextToken(session.getRestaurant().getId()));
        }
        order.setTotalAmount(BigDecimal.ZERO);
        Order savedOrder = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemRequest.getMenuItemId())
                    .orElseThrow(() -> new RuntimeException("Menu item not found"));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPriceAtOrder(menuItem.getPrice());
            orderItemRepository.save(orderItem);

            BigDecimal lineTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            total = total.add(lineTotal);
        }

        savedOrder.setTotalAmount(total);
        Order finalOrder = orderRepository.save(savedOrder);

        // A dine-in order confirms the guest's table and keeps it reserved a while longer
        if (!"TAKEAWAY".equals(session.getOrderType()) && session.getTableNumber() != null) {
            Long rid = session.getRestaurant().getId();
            tableRepository.findAll().stream()
                    .filter(t -> t.getRestaurant() != null && rid.equals(t.getRestaurant().getId())
                            && session.getTableNumber().equals(t.getTableNumber()))
                    .findFirst()
                    .ifPresent(t -> {
                        t.setIsOccupied(true);
                        t.setOrderConfirmed(true);
                        t.setLastActivity(LocalDateTime.now());
                        tableRepository.save(t);
                    });
        }

        // Push to any kitchen dashboard listening for this restaurant
        messagingTemplate.convertAndSend(
            "/topic/kitchen/" + finalOrder.getRestaurant().getId(),
            finalOrder
        );

        return finalOrder;
    }

    // Next pickup number for today: highest token used today + 1
    private int nextToken(Long restaurantId) {
        java.time.LocalDate today = LocalDate.now();
        int max = orderRepository.findAll().stream()
                .filter(o -> o.getRestaurant() != null && o.getRestaurant().getId().equals(restaurantId))
                .filter(o -> o.getTokenNumber() != null && o.getCreatedAt() != null
                        && o.getCreatedAt().toLocalDate().equals(today))
                .mapToInt(Order::getTokenNumber)
                .max().orElse(0);
        return max + 1;
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id, @RequestParam(required = false) String key) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
        if (order.getTableSession() == null || !order.getTableSession().keyMatches(key)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return order;
    }

    // This is the important one: no restaurantId parameter needed anymore.
    // It's read straight from the logged-in kitchen staff member's JWT.
    @GetMapping("/kitchen")
    public List<Order> getKitchenOrders() {
        Long restaurantId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();

        return orderRepository.findAll().stream()
                .filter(order -> order.getRestaurant() != null && order.getRestaurant().getId().equals(restaurantId))
                .collect(Collectors.toList());
    }

    @PatchMapping("/{id}/status")
    public Order updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        Long restaurantId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        String chefName = SecurityContextHolder.getContext().getAuthentication().getName();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getRestaurant() == null || !order.getRestaurant().getId().equals(restaurantId)) {
            throw new RuntimeException("You do not have permission to update this order");
        }

        // A chef who starts a waiting order takes it as their own
        if ("PREPARING".equals(request.getStatus()) && order.getAssignedChef() == null) {
            KitchenUser me = chefAssignmentService.findChef(restaurantId, chefName);
            if (me != null) {
                order.setAssignedChef(me);
            }
        }

        order.setStatus(request.getStatus());
        Order saved = orderRepository.save(order);

        // A finished order frees up room: give waiting orders to whoever has space
        if ("SERVED".equals(request.getStatus()) || "READY".equals(request.getStatus())) {
            chefAssignmentService.assignWaitingOrders(restaurantId);
        }

        return saved;
    }
}
