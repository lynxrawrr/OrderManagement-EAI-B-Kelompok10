package com.example.inventoryservice.listener;

import com.example.inventoryservice.dto.OrderEvent;
import com.example.inventoryservice.service.InventoryService;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    @Autowired
    private InventoryService inventoryService;

    // Mendengarkan event produk baru untuk inisialisasi baris stok di database inventory
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "product_inventory_queue", durable = "true"),
        exchange = @Exchange(value = "order_exchange", type = "topic"),
        key = "product_sync_key"
    ))
    public void consumeProductSyncEvent(OrderEvent event) {
        System.out.println("Menerima pesan sinkronisasi produk baru! ID: " + event.getProductId());
        try {
            inventoryService.createInventory(event.getProductId(), event.getQuantity());
        } catch (Exception e) {
            System.err.println("Gagal sinkronisasi produk: " + e.getMessage());
        }
    }

    // Mendengarkan event order baru untuk mengunci (reserve) stok
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

    // Mendengarkan event cancel order untuk melepas kembali stok ke status tersedia
    @RabbitListener(queuesToDeclare = @Queue("order_cancel_queue"))
    public void consumeCancelEvent(OrderEvent event) {
        System.out.println("Menerima pesan CANCEL! Melepas (release) stok...");
        inventoryService.releaseStock(event.getProductId(), event.getQuantity());
    }

    // Mendengarkan event shipped (barang dikirim) untuk menghapus stok dari daftar cadangan
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "order_shipped_inventory_queue", durable = "true"),
        exchange = @Exchange(value = "order_exchange", type = "topic"),
        key = "order_shipped_key"
    ))
    public void consumeShippedEvent(OrderEvent event) {
        System.out.println("Pesanan telah dikirim! Mengurangi reserved stock...");
        inventoryService.finalizeStock(event.getProductId(), event.getQuantity());
    }
}