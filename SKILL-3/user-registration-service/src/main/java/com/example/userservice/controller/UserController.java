package com.example.userservice.controller;

import com.example.userservice.entity.User;
import com.example.userservice.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // =========================
    // USER REGISTRATION
    // =========================
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody User user) {

        try {
            String result = userService.register(user);

            return ResponseEntity.ok(
                    Map.of("message", result)
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error", e.getMessage()
                    ));
        }
    }

    // =========================
    // USER LOGIN
    // =========================
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        try {
            String result = userService.login(
                    request.username(),
                    request.password()
            );

            return ResponseEntity.ok(
                    Map.of("message", result)
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(401)
                    .body(Map.of(
                            "error", e.getMessage()
                    ));
        }
    }

    // =========================
    // LOGIN REQUEST
    // =========================
    public record LoginRequest(

            @NotBlank(message = "Username is required")
            String username,

            @NotBlank(message = "Password is required")
            String password

    ) {
    }
}