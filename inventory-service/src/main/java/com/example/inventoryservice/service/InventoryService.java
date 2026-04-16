package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {
    @Autowired
    private InventoryRepository inventoryRepository;

    public List<Inventory> getAllStock() {
        return inventoryRepository.findAll();
    }

    public Inventory getStock(Long productId) {
        return inventoryRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan di Inventory"));
    }

    @Transactional
    public void createInventory(Long productId, Integer initialStock) {
        Inventory inv = new Inventory();
        inv.setProductId(productId);
        inv.setAvailableStock(initialStock);
        inv.setReservedStock(0);
        inventoryRepository.save(inv);
        System.out.println("Inventory awal dibuat untuk Produk ID: " + productId);
    }

    // Metode untuk mengunci stok sementara saat pesanan dibuat (Reserved)
    @Transactional
    public void reserveStock(Long productId, Integer quantity) {
        Inventory inv = getStock(productId);
        if (inv.getAvailableStock() < quantity) {
            throw new RuntimeException("Stok tidak cukup untuk di-reserve!");
        }
        inv.setAvailableStock(inv.getAvailableStock() - quantity); // Kurangi stok tersedia
        inv.setReservedStock(inv.getReservedStock() + quantity);   // Tambah stok cadangan
        inventoryRepository.save(inv);
    }

    // Metode untuk menghapus stok cadangan saat barang sudah benar-benar dikirim (Shipped)
    @Transactional
    public void finalizeStock(Long productId, Integer quantity) {
        Inventory inv = getStock(productId);
        int currentReserved = inv.getReservedStock();

        if (currentReserved >= quantity) {
            // Alur Normal: Kurangi dari stok cadangan (hasil dari reserveStock sebelumnya)
            inv.setReservedStock(currentReserved - quantity);
            System.out.println("Stok FINALIZED (Normal)! Mengurangi Reserved Stock untuk Produk ID: " + productId);
        } else {
            // Alur Manual/Revisi: Cadangan tidak cukup (misal 0 karena lompat flow), kurangi sisa dari stok tersedia
            int shortage = quantity - currentReserved;
            inv.setReservedStock(0);
            inv.setAvailableStock(inv.getAvailableStock() - shortage);
            System.out.println("Peringatan: Stok cadangan tidak cukup. Mengurangi " + shortage + " dari Available Stock untuk Produk ID: " + productId);
        }

        inventoryRepository.save(inv);
        System.out.println("Stok FINALIZED untuk Produk ID: " + productId);
    }

    // Metode untuk mengembalikan stok cadangan ke tersedia jika pesanan dibatalkan (Cancel)
    @Transactional
    public void releaseStock(Long productId, Integer quantity) {
        Inventory inv = getStock(productId);
        inv.setReservedStock(inv.getReservedStock() - quantity); // Kurangi cadangan
        inv.setAvailableStock(inv.getAvailableStock() + quantity); // Kembalikan ke tersedia
        inventoryRepository.save(inv);
    }
}