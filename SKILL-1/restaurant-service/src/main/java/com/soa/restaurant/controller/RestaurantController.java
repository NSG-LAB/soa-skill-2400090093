package com.soa.restaurant.controller;

import com.soa.restaurant.model.Restaurant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurants")
public class RestaurantController {

    private final List<Restaurant> restaurants = List.of(
        new Restaurant(1, "Paradise Restaurant", "Vijayawada"),
        new Restaurant(2, "Bawarchi", "Hyderabad"),
        new Restaurant(3, "Mehfil", "Hyderabad")
    );

    @GetMapping
    public List<Restaurant> getRestaurants() {
        return restaurants;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Restaurant> getRestaurant(@PathVariable int id) {

        return restaurants.stream()
            .filter(r -> r.getId() == id)
            .findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
