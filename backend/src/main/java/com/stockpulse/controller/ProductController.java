package com.stockpulse.controller;

import com.stockpulse.model.Category;
import com.stockpulse.model.Product;
import com.stockpulse.model.ProductStatus;
import com.stockpulse.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> getProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Category category) {
        return productService.getProducts(status, category);
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Product> updateStock(@PathVariable String id, @RequestBody Map<String, Integer> payload) {
        int newStock = payload.get("stockLevel");
        Product updated = productService.updateStock(id, newStock);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/orders")
    public ResponseEntity<Product> simulateSale(@PathVariable String id, @RequestBody(required = false) Map<String, Integer> payload) {
        int quantity = (payload != null && payload.containsKey("quantity")) ? payload.get("quantity") : 1;
        Product updated = productService.simulateSale(id, quantity);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/suggest-pricing")
    public ResponseEntity<Void> suggestPricing(@PathVariable String id) {
        productService.triggerManualPricing(id);
        return ResponseEntity.accepted().build();
    }
}
