package com.example.ordermanagement.controller;

import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

// Exception
import jakarta.validation.Valid;
import com.example.ordermanagement.dto.ProductRequest;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts()); 
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Optional<Product> product = productService.getProductById(id); 
        return product.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build()); 
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody @Valid ProductRequest request) {
        Product product = new Product(); 
        product.setName(request.getName()); 
        product.setPrice(request.getPrice()); 
        product.setStock(request.getStock()); 
        
        Product saved = productService.createProduct(product); 
        return new ResponseEntity<>(saved, HttpStatus.CREATED); 
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product request) {
        Product updated = productService.updateProduct(id, request); 
        if (updated == null) {
            return ResponseEntity.notFound().build(); 
        }
        return ResponseEntity.ok(updated); 
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        boolean deleted = productService.deleteProduct(id); 
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build(); 
    }
}