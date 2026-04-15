package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    @Autowired
    private InventoryRepository inventoryRepository;

    public Inventory getStock(Long productId) {
        return inventoryRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan di Inventory"));
    }

    @Transactional
    public void reserveStock(Long productId, Integer quantity) {
        Inventory inv = getStock(productId);
        if (inv.getAvailableStock() < quantity) {
            throw new RuntimeException("Stok tidak cukup untuk di-reserve!");
        }
        inv.setAvailableStock(inv.getAvailableStock() - quantity);
        inv.setReservedStock(inv.getReservedStock() + quantity);
        inventoryRepository.save(inv);
    }

    @Transactional
    public void releaseStock(Long productId, Integer quantity) {
        Inventory inv = getStock(productId);
        inv.setReservedStock(inv.getReservedStock() - quantity);
        inv.setAvailableStock(inv.getAvailableStock() + quantity);
        inventoryRepository.save(inv);
    }
}