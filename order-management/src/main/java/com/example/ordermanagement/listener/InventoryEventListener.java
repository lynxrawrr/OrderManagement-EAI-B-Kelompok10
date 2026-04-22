package com.example.ordermanagement.listener;

import com.example.ordermanagement.dto.OrderEvent;
import com.example.ordermanagement.service.OrderService;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventListener {

    @Autowired
    private OrderService orderService;

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "order_reserve_failed_queue", durable = "true"),
        exchange = @Exchange(value = "order_exchange", type = "topic"),
        key = "order_reserve_failed_key"
    ))
    public void consumeReserveFailed(OrderEvent event) {
        System.out.println("Menerima reserve gagal dari Inventory untuk Order ID: " + event.getOrderId());
        orderService.markAsFailedFromInventory(event.getOrderId());
    }
}
