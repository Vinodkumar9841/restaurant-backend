package com.restaurant.restaurant_backend.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.restaurant.restaurant_backend.dto.CreatePaymentRequest;
import com.restaurant.restaurant_backend.dto.VerifyPaymentRequest;
import com.restaurant.restaurant_backend.model.Payment;
import com.restaurant.restaurant_backend.model.TableSession;
import com.restaurant.restaurant_backend.repository.PaymentRepository;
import com.restaurant.restaurant_backend.repository.TableSessionRepository;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private final PaymentRepository paymentRepository;
    private final TableSessionRepository tableSessionRepository;

    public PaymentController(PaymentRepository paymentRepository, TableSessionRepository tableSessionRepository) {
        this.paymentRepository = paymentRepository;
        this.tableSessionRepository = tableSessionRepository;
    }

    @PostMapping
    public Map<String, Object> createPayment(@RequestBody CreatePaymentRequest request) throws Exception {
        TableSession session = tableSessionRepository.findById(request.getTableSessionId())
                .orElseThrow(() -> new RuntimeException("Table session not found"));

        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        int amountInPaise = request.getAmount().multiply(java.math.BigDecimal.valueOf(100)).intValue();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "table_" + session.getTableNumber() + "_" + System.currentTimeMillis());

        Order razorpayOrder = razorpay.orders.create(orderRequest);

        Payment payment = new Payment();
        payment.setTableSession(session);
        payment.setRestaurant(session.getRestaurant());
        payment.setRazorpayOrderId(razorpayOrder.get("id"));
        payment.setAmount(request.getAmount());
        payment.setStatus("CREATED");
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Map<String, Object> response = new HashMap<>();
        response.put("razorpayOrderId", razorpayOrder.get("id"));
        response.put("amount", amountInPaise);
        response.put("currency", "INR");
        response.put("razorpayKeyId", razorpayKeyId);

        return response;
    }

    @PostMapping("/verify")
    public Map<String, Object> verifyPayment(@RequestBody VerifyPaymentRequest request) throws Exception {
        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", request.getRazorpayOrderId());
        options.put("razorpay_payment_id", request.getRazorpayPaymentId());
        options.put("razorpay_signature", request.getRazorpaySignature());

        boolean isValid = com.razorpay.Utils.verifyPaymentSignature(options, razorpayKeySecret);

        Map<String, Object> response = new HashMap<>();

        if (isValid) {
            Payment payment = paymentRepository.findAll().stream()
                    .filter(p -> request.getRazorpayOrderId().equals(p.getRazorpayOrderId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Payment record not found"));

            payment.setStatus("PAID");
            paymentRepository.save(payment);

            response.put("verified", true);
            response.put("paymentId", payment.getId());
        } else {
            response.put("verified", false);
        }

        return response;
    }

    @PostMapping("/cash")
    public Map<String, Object> createCashPayment(@RequestBody CreatePaymentRequest request) {
        TableSession session = tableSessionRepository.findById(request.getTableSessionId())
                .orElseThrow(() -> new RuntimeException("Table session not found"));

        Payment payment = new Payment();
        payment.setTableSession(session);
        payment.setRestaurant(session.getRestaurant());
        payment.setRazorpayOrderId(null);
        payment.setAmount(request.getAmount());
        payment.setStatus("CASH_PENDING");
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Map<String, Object> response = new HashMap<>();
        response.put("paymentId", payment.getId());
        response.put("method", "cash");

        return response;
    }
}