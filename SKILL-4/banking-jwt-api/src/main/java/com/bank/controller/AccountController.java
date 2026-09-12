package com.bank.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/account")
public class AccountController {

    @GetMapping("/details")
    public Map<String, Object> accountDetails(
            Authentication authentication
    ) {

        return Map.of(
                "accountNumber", "XXXX-XXXX-1234",
                "accountHolder", authentication.getName(),
                "accountType", "Savings",
                "balance", 75000.00,
                "currency", "INR",
                "status", "ACTIVE"
        );
    }
}
