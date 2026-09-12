package com.soa.product.controller;

import com.soa.product.model.Product;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductController {

    @GetMapping("/products")
    public List<Product> getProducts() {

        return List.of(
            new Product(1, "Laptop", 55000),
            new Product(2, "Smartphone", 25000),
            new Product(3, "Headphones", 3000)
        );
    }
}
