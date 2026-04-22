package com.example.shippingservice.listener;

import com.example.shippingservice.dto.OrderEvent;
import com.example.shippingservice.service.ShipmentService;
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
    private final Set<Long> closedOrders = ConcurrentHashMap.newKeySet();

    @Autowired
    private ShipmentService shipmentService;

    // Shipment baru dibuat hanya setelah Inventory berhasil reserve stok
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "order_shipping_queue", durable = "true"),
        exchange = @Exchange(value = "order_exchange", type = "topic"),
        key = "stock_reserved_key"
    ))
    public void consumeOrderEvent(OrderEvent event) {
        if (closedOrders.contains(event.getOrderId())) {
            System.out.println("Melewati create shipment karena order sudah ditutup: " + event.getOrderId());
            return;
        }

        System.out.println("Menerima pesanan baru! Memproses jadwal pengiriman untuk Order ID: " + event.getOrderId());
        try {
            // Panggil fungsi create shipment dengan data lengkap
            shipmentService.createShipment(event.getOrderId(), event.getProductId(), event.getQuantity());
            System.out.println("Berhasil membuat jadwal pengiriman!");
        } catch (Exception e) {
            System.err.println("Gagal membuat pengiriman: " + e.getMessage());
        }
    }

    // Mendengarkan event cancel order untuk membatalkan pengiriman
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "order_shipping_cancel_queue", durable = "true"),
        exchange = @Exchange(value = "order_exchange", type = "topic"),
        key = "order_cancel_key"
    ))
    public void consumeCancelEvent(OrderEvent event) {
        closedOrders.add(event.getOrderId());
        System.out.println("Menerima pembatalan pesanan untuk Order ID: " + event.getOrderId());
        try {
            shipmentService.cancelShipment(event.getOrderId());
            System.out.println("Berhasil memproses pembatalan pengiriman.");
        } catch (Exception e) {
            System.err.println("Gagal membatalkan pengiriman: " + e.getMessage());
        }
    }
}
