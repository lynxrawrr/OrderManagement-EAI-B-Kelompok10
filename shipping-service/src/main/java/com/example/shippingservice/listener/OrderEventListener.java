package com.example.shippingservice.listener;

import com.example.shippingservice.dto.OrderEvent;
import com.example.shippingservice.service.ShipmentService;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    @Autowired
    private ShipmentService shipmentService;

    // Mendengarkan Exchange yang sama dari OrderService, tapi pakai Queue khusus Shipping
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "order_shipping_queue", durable = "true"),
        exchange = @Exchange(value = "order_exchange", type = "topic"),
        key = "order_routing_key"
    ))
    public void consumeOrderEvent(OrderEvent event) {
        System.out.println("Menerima pesanan baru! Memproses jadwal pengiriman untuk Order ID: " + event.getOrderId());
        try {
            // Panggil fungsi create shipment dengan data lengkap
            shipmentService.createShipment(event.getOrderId(), event.getProductId(), event.getQuantity());
            System.out.println("Berhasil membuat jadwal pengiriman!");
        } catch (Exception e) {
            System.err.println("Gagal membuat pengiriman: " + e.getMessage());
        }
    }
}