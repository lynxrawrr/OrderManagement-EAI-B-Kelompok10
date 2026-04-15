package com.example.inventoryservice.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    // Kita hanya butuh converter di sini agar bisa membaca JSON dari Producer
    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }
}