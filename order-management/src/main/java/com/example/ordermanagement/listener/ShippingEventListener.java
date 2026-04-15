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
public class ShippingEventListener {

    @Autowired
    private OrderService orderService;

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "order_status_queue", durable = "true"),
        exchange = @Exchange(value = "order_exchange", type = "topic"),
        key = "order_status_update_key"
    ))
    public void consumeStatusUpdate(OrderEvent event) {
        System.out.println("Menerima update status dari Shipping untuk Order ID: " + event.getOrderId());
        orderService.updateStatus(event.getOrderId(), "SHIPPED");
    }
}