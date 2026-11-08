package com.restaurant.restaurant_backend.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.restaurant_backend.model.RestaurantTable;
import com.restaurant.restaurant_backend.repository.RestaurantTableRepository;

@RestController
@CrossOrigin(origins = "*")
public class TableStatusController {

    private final RestaurantTableRepository tableRepository;

    public TableStatusController(RestaurantTableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    // A table that nobody has picked in a while is treated as free again:
    //  - picked but no order yet: held for 30 minutes
    //  - guest has ordered: held for 90 minutes after the last activity
    private static final long HOLD_MINUTES = 30;
    private static final long IDLE_MINUTES = 90;

    private boolean isTaken(RestaurantTable t) {
        if (!Boolean.TRUE.equals(t.getIsOccupied())) return false;
        if (t.getLastActivity() == null) return false;
        long limit = Boolean.TRUE.equals(t.getOrderConfirmed()) ? IDLE_MINUTES : HOLD_MINUTES;
        return t.getLastActivity().plusMinutes(limit).isAfter(LocalDateTime.now());
    }

    private List<RestaurantTable> tablesOf(Long restaurantId) {
        return tableRepository.findAll().stream()
                .filter(t -> t.getRestaurant() != null && restaurantId.equals(t.getRestaurant().getId()))
                .sorted(Comparator.comparing(RestaurantTable::getTableNumber))
                .collect(Collectors.toList());
    }

    // Guests see this after scanning the entrance QR: every table, free or occupied, plus the totals
    @GetMapping("/api/public/restaurants/{restaurantId}/tables")
    public Map<String, Object> publicTables(@PathVariable Long restaurantId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int occupied = 0;
        for (RestaurantTable t : tablesOf(restaurantId)) {
            Map<String, Object> row = new HashMap<>();
            row.put("tableNumber", t.getTableNumber());
            row.put("occupied", isTaken(t));
            if (isTaken(t)) occupied++;
            rows.add(row);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("tables", rows);
        result.put("occupied", occupied);
        result.put("free", rows.size() - occupied);
        return result;
    }

    // A guest sits down: the table becomes occupied.
    // force=true is used when the guest scanned a QR code stuck on that table (they are definitely sitting there).
    @PostMapping("/api/public/restaurants/{restaurantId}/tables/{tableNumber}/claim")
    public ResponseEntity<Map<String, Object>> claim(@PathVariable Long restaurantId,
                                                     @PathVariable Integer tableNumber,
                                                     @RequestParam(defaultValue = "false") boolean force) {
        Map<String, Object> body = new HashMap<>();
        RestaurantTable table = tablesOf(restaurantId).stream()
                .filter(t -> tableNumber.equals(t.getTableNumber()))
                .findFirst()
                .orElse(null);
        if (table == null) {
            body.put("message", "Table not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        if (isTaken(table) && !force) {
            body.put("message", "This table is already taken");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }
        table.setIsOccupied(true);
        table.setLastActivity(LocalDateTime.now());
        table.setOrderConfirmed(false);
        tableRepository.save(table);
        body.put("message", "ok");
        return ResponseEntity.ok(body);
    }

    // The guest taps "I'm done": the table becomes free right away
    @PostMapping("/api/public/restaurants/{restaurantId}/tables/{tableNumber}/release")
    public ResponseEntity<Map<String, Object>> release(@PathVariable Long restaurantId,
                                                       @PathVariable Integer tableNumber) {
        Map<String, Object> body = new HashMap<>();
        RestaurantTable table = tablesOf(restaurantId).stream()
                .filter(t -> tableNumber.equals(t.getTableNumber()))
                .findFirst()
                .orElse(null);
        if (table == null) {
            body.put("message", "Table not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        table.setIsOccupied(false);
        table.setOrderConfirmed(false);
        tableRepository.save(table);
        body.put("message", "ok");
        return ResponseEntity.ok(body);
    }

    // The owner marks a table free (guests left) or occupied
    @PatchMapping("/api/owner/tables/{id}/occupied")
    public Map<String, Object> setOccupied(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long restaurantId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found"));
        if (table.getRestaurant() == null || !table.getRestaurant().getId().equals(restaurantId)) {
            throw new RuntimeException("You do not have permission to change this table");
        }
        table.setIsOccupied(Boolean.parseBoolean(String.valueOf(body.get("occupied"))));
        table.setLastActivity(LocalDateTime.now());
        table.setOrderConfirmed(true);
        tableRepository.save(table);

        Map<String, Object> result = new HashMap<>();
        result.put("id", table.getId());
        result.put("occupied", isTaken(table));
        return result;
    }
}
