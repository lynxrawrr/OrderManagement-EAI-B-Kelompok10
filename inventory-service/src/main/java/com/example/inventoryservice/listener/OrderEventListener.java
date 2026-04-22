package com.example.inventoryservice.listener;

import com.example.inventoryservice.dto.OrderEvent;
import com.example.inventoryservice.service.InventoryService;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderEventListener {
    private static final String EXCHANGE = "order_exchange";
    private static final String STOCK_RESERVED_ROUTING_KEY = "stock_reserved_key";
    private static final String RESERVE_FAILED_ROUTING_KEY = "order_reserve_failed_key";

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final Set<Long> closedOrders = ConcurrentHashMap.newKeySet();

    // Mendengarkan event sinkronisasi produk untuk inisialisasi atau update total stok
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "product_inventory_queue", durable = "true"),
        exchange = @Exchange(value = "order_exchange", type = "topic"),
        key = "product_sync_key"
    ))
    public void consumeProductSyncEvent(OrderEvent event) {
        System.out.println("Menerima pesan sinkronisasi produk! ID: " + event.getProductId());
        try {
            inventoryService.syncProductStock(event.getProductId(), event.getQuantity());
        } catch (Exception e) {
            System.err.println("Gagal sinkronisasi produk: " + e.getMessage());
        }
    }

    // Mendengarkan event penghapusan produk agar inventory ikut dibersihkan
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "product_inventory_delete_queue", durable = "true"),
        exchange = @Exchange(value = "order_exchange", type = "topic"),
        key = "product_delete_key"
    ))
    public void consumeProductDeleteEvent(OrderEvent event) {
        System.out.println("Menerima pesan penghapusan produk! ID: " + event.getProductId());
        try {
            inventoryService.deleteInventory(event.getProductId());
        } catch (Exception e) {
            System.err.println("Gagal hapus inventory untuk Produk ID " + event.getProductId() + ": " + e.getMessage());
        }
    }

    // Mendengarkan event order baru untuk mengunci (reserve) stok
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "order_stock_queue", durable = "true"),
        exchange = @Exchange(value = "order_exchange", type = "topic"),
        key = "order_routing_key"
    ))
    public void consumeOrderEvent(OrderEvent event) {
        if (closedOrders.contains(event.getOrderId())) {
            System.out.println("Melewati reserve stok karena order sudah ditutup: " + event.getOrderId());
            return;
        }

        System.out.println("Menerima pesan dari RabbitMQ! Memproses reserve stok...");
        try {
            inventoryService.reserveStock(event.getProductId(), event.getQuantity());
            System.out.println("Berhasil reserve stok untuk Produk ID: " + event.getProductId() + " sebanyak "
                    + event.getQuantity());
            rabbitTemplate.convertAndSend(EXCHANGE, STOCK_RESERVED_ROUTING_KEY, event);
            System.out.println("Mengirim event stock reserved untuk Order ID: " + event.getOrderId());
        } catch (Exception e) {
            closedOrders.add(event.getOrderId());
            System.err.println("Gagal reserve stok: " + e.getMessage());
            rabbitTemplate.convertAndSend(EXCHANGE, RESERVE_FAILED_ROUTING_KEY, event);
            System.out.println("Mengirim event reserve failed untuk Order ID: " + event.getOrderId());
        }

    }

    // Mendengarkan event cancel order untuk melepas kembali stok ke status tersedia
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "order_cancel_queue", durable = "true"),
        exchange = @Exchange(value = "order_exchange", type = "topic"),
        key = "order_cancel_key"
    ))
    public void consumeCancelEvent(OrderEvent event) {
        closedOrders.add(event.getOrderId());
        System.out.println("Menerima pesan CANCEL untuk Produk ID: " + event.getProductId() + " Qty: " + event.getQuantity());
        try {
            inventoryService.releaseStock(event.getProductId(), event.getQuantity());
            System.out.println("Berhasil release stok untuk Produk ID: " + event.getProductId());
        } catch (Exception e) {
            System.err.println("Gagal release stok untuk Produk ID " + event.getProductId() + ": " + e.getMessage());
        }
    }

    // Mendengarkan event shipped (barang dikirim) untuk menghapus stok dari daftar cadangan
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "order_shipped_inventory_queue", durable = "true"),
        exchange = @Exchange(value = "order_exchange", type = "topic"),
        key = "order_shipped_key"
    ))
    public void consumeShippedEvent(OrderEvent event) {
        closedOrders.add(event.getOrderId());
        System.out.println("Pesanan telah dikirim! Mengurangi reserved stock...");
        inventoryService.finalizeStock(event.getProductId(), event.getQuantity());
    }
}
