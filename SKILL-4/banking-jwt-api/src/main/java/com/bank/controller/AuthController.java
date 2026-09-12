package com.bank.controller;

import com.bank.model.LoginRequest;
import com.bank.model.LoginResponse;
import com.bank.model.User;
import com.bank.service.AuthService;
import com.bank.service.JwtService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(
            AuthService authService,
            JwtService jwtService
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request
    ) {

        User user = authService.authenticate(
                request.getUsername(),
                request.getPassword()
        );

        String token = jwtService.generateToken(
                user.getUsername()
        );

        return ResponseEntity.ok(
                new LoginResponse(token)
        );
    }
}
