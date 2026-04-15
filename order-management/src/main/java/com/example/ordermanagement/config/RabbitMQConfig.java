package com.example.ordermanagement.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Nama antrean dan kunci pengiriman pesan
    public static final String QUEUE = "order_stock_queue";
    public static final String CANCEL_QUEUE = "order_cancel_queue";
    public static final String EXCHANGE = "order_exchange";
    public static final String ROUTING_KEY = "order_routing_key";
    public static final String CANCEL_ROUTING_KEY = "order_cancel_key";
    public static final String PRODUCT_SYNC_ROUTING_KEY = "product_sync_key";

    // Antrean untuk memproses pengurangan stok pesanan
    @Bean
    public Queue queue() {
        return new Queue(QUEUE);
    }

    // Antrean untuk membatalkan pesanan (release stok)
    @Bean
    public Queue cancelQueue() {
        return new Queue(CANCEL_QUEUE);
    }

    // Antrean untuk sinkronisasi data produk baru ke service inventory
    @Bean
    public Queue productSyncQueue() {
        return new Queue("product_inventory_queue");
    }

    // Exchange (Kantor Pos) tempat pesan dikirim sebelum diarahkan ke antrean yang tepat
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    // Menghubungkan antrean order dengan exchange menggunakan routing key
    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    // Menghubungkan antrean cancel dengan exchange menggunakan routing key khusus pembatalan
    @Bean
    public Binding cancelBinding(Queue cancelQueue, TopicExchange exchange) {
        return BindingBuilder.bind(cancelQueue).to(exchange).with(CANCEL_ROUTING_KEY);
    }

    // Menghubungkan antrean produk dengan exchange menggunakan routing key khusus sinkronisasi
    @Bean
    public Binding productSyncBinding(Queue productSyncQueue, TopicExchange exchange) {
        return BindingBuilder.bind(productSyncQueue).to(exchange).with(PRODUCT_SYNC_ROUTING_KEY);
    }

    // Mengubah pesan Java menjadi JSON agar mudah dibaca di RabbitMQ
    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate template(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
}