#!/usr/bin/env bash
set -e

echo "🔧 Fixing User Registration validation..."

BASE="src/main/java/com/example/userservice"

# -----------------------------
# User.java
# -----------------------------
cat > "$BASE/entity/User.java" <<'JAVA'
package com.example.userservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
    }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;

    public User() {
    }

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
JAVA

# -----------------------------
# UserController.java
# -----------------------------
cat > "$BASE/controller/UserController.java" <<'JAVA'
package com.example.userservice.controller;

import com.example.userservice.entity.User;
import com.example.userservice.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

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
                    .body(Map.of("error", e.getMessage()));
        }
    }

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
                    .body(Map.of("error", e.getMessage()));
        }
    }

    public record LoginRequest(

            @NotBlank(message = "Username is required")
            String username,

            @NotBlank(message = "Password is required")
            String password

    ) {
    }
}
JAVA

# -----------------------------
# GlobalExceptionHandler.java
# -----------------------------
cat > "$BASE/exception/GlobalExceptionHandler.java" <<'JAVA'
package com.example.userservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        return ResponseEntity
                .badRequest()
                .body(errors);
    }
}
JAVA

echo "✅ Validation files updated."

echo "🧹 Cleaning project..."
mvn clean package -DskipTests

echo ""
echo "✅ BUILD COMPLETE"
echo ""
echo "Start the application with:"
echo "java -jar target/user-registration-service-1.0.0.jar"
