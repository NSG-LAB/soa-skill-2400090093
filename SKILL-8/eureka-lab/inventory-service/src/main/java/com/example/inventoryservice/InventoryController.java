package com.example.inventoryservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class InventoryController {

    @Value("${server.port}")
    private int port;

    @Value("${spring.application.name:inventory-service}")
    private String serviceName;

    @GetMapping("/inventory")
    public Map<String, Object> getInventoryStatus() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", serviceName);
        response.put("instance", "inventory-" + port);
        response.put("port", port);
        response.put("status", "Inventory service is running");
        return response;
    }

    @GetMapping("/inventory/{id}")
    public Map<String, Object> getInventoryItem(@PathVariable("id") String id) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", serviceName);
        response.put("instance", "inventory-" + port);
        response.put("port", port);
        response.put("id", id);
        response.put("item", "Item-" + id);
        response.put("stock", 100);
        response.put("status", "Available");
        return response;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", serviceName);
        response.put("port", port);
        return response;
    }
}
