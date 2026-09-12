package com.soa.order.controller;

import com.soa.order.model.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody Order order) {

        String restaurantUrl =
                "http://localhost:8081/restaurants/" + order.getRestaurantId();

        try {
            restTemplate.getForEntity(restaurantUrl, Object.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of(
                    "message", "Restaurant not found",
                    "restaurantId", order.getRestaurantId(),
                    "status", "REJECTED"
                )
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(
            Map.of(
                "message", "Order placed successfully",
                "orderId", 1001,
                "userId", order.getUserId(),
                "restaurantId", order.getRestaurantId(),
                "items", order.getItems(),
                "status", "CONFIRMED"
            )
        );
    }
}
