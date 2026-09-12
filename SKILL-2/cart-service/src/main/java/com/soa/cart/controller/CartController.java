package com.soa.cart.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class CartController {

    @GetMapping("/cart")
    public Map<String, Object> getCart() {

        return Map.of(
            "userId", 101,
            "items", List.of(
                "Laptop",
                "Headphones"
            ),
            "message", "Cart retrieved successfully"
        );
    }
}
