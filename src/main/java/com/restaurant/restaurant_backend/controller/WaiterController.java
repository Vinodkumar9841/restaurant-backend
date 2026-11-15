package com.restaurant.restaurant_backend.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.restaurant_backend.model.TableSession;
import com.restaurant.restaurant_backend.repository.TableSessionRepository;

// "Call waiter": a guest taps the button, the owner sees it and marks it done.
@RestController
@CrossOrigin(origins = "*")
public class WaiterController {

    private final TableSessionRepository sessionRepository;

    public WaiterController(TableSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @PostMapping("/api/public/sessions/{sessionId}/call-waiter")
    public Map<String, Object> callWaiter(@PathVariable Long sessionId, @RequestParam(required = false) String key) {
        TableSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
        if (!session.keyMatches(key)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        if (!Boolean.TRUE.equals(session.getWaiterCalled())) {
            session.setWaiterCalled(true);
            session.setWaiterCalledAt(LocalDateTime.now());
            sessionRepository.save(session);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("called", true);
        return result;
    }

    @GetMapping("/api/owner/waiter-calls")
    public List<Map<String, Object>> openCalls() {
        Long restaurantId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TableSession s : sessionRepository.findAll()) {
            if (s.getRestaurant() == null || !restaurantId.equals(s.getRestaurant().getId())) continue;
            if (!Boolean.TRUE.equals(s.getWaiterCalled()) || Boolean.TRUE.equals(s.getSettled())) continue;
            Map<String, Object> row = new HashMap<>();
            row.put("sessionId", s.getId());
            row.put("tableNumber", s.getTableNumber());
            row.put("customerName", s.getCustomerName());
            row.put("calledAt", s.getWaiterCalledAt() != null ? s.getWaiterCalledAt().toString() : null);
            rows.add(row);
        }
        rows.sort(Comparator.comparing((Map<String, Object> r) -> String.valueOf(r.get("calledAt"))));
        return rows;
    }

    @PostMapping("/api/owner/sessions/{sessionId}/waiter-done")
    public Map<String, Object> done(@PathVariable Long sessionId) {
        Long restaurantId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        TableSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (session.getRestaurant() == null || !restaurantId.equals(session.getRestaurant().getId())) {
            throw new RuntimeException("You do not have permission for this table");
        }
        session.setWaiterCalled(false);
        sessionRepository.save(session);
        Map<String, Object> result = new HashMap<>();
        result.put("done", true);
        return result;
    }
}
