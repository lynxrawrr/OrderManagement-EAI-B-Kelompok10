package com.example.inventoryservice.listener;

import com.example.inventoryservice.dto.OrderEvent;
import com.example.inventoryservice.service.InventoryService;

import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    @Autowired
    private InventoryService inventoryService;

    // Mendengarkan antrean yang sama dengan yang dibuat oleh Producer
    // KUNCI PERBAIKANNYA ADA DI BARIS INI:
    // Kita gunakan 'queuesToDeclare' untuk memaksa pembuatan antrean jika belum ada
    @RabbitListener(queuesToDeclare = @Queue("order_stock_queue"))
    public void consumeOrderEvent(OrderEvent event) {
        System.out.println("Menerima pesan dari RabbitMQ! Memproses reserve stok...");
        try {
            inventoryService.reserveStock(event.getProductId(), event.getQuantity());
            System.out.println("Berhasil reserve stok untuk Produk ID: " + event.getProductId() + " sebanyak "
                    + event.getQuantity());
        } catch (Exception e) {
            System.err.println("Gagal reserve stok: " + e.getMessage());
        }

    }

    @RabbitListener(queuesToDeclare = @Queue("order_cancel_queue"))
    public void consumeCancelEvent(OrderEvent event) {
        System.out.println("Menerima pesan CANCEL! Melepas (release) stok...");
        inventoryService.releaseStock(event.getProductId(), event.getQuantity());
    }
}