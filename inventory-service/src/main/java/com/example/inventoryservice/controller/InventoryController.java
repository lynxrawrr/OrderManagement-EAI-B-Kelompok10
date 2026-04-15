package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.InventoryRequest;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    @Autowired
    private InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<Inventory>> getAllStock() {
        return ResponseEntity.ok(inventoryService.getAllStock());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Inventory> getStock(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getStock(productId));
    }

    @PostMapping("/reserve")
    public ResponseEntity<String> reserve(@RequestBody InventoryRequest request) {
        inventoryService.reserveStock(request.getProductId(), request.getQuantity());
        return ResponseEntity.ok("Stok berhasil di-reserve via JSON");
    }

    @PostMapping("/release")
    public ResponseEntity<String> release(@RequestBody InventoryRequest request) {
        inventoryService.releaseStock(request.getProductId(), request.getQuantity());
        return ResponseEntity.ok("Stok berhasil di-release via JSON");
    }
}