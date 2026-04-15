package com.example.ordermanagement.service;

import com.example.ordermanagement.config.RabbitMQConfig;
import com.example.ordermanagement.dto.OrderEvent;
import com.example.ordermanagement.entity.Customer;
import com.example.ordermanagement.entity.Order;
import com.example.ordermanagement.entity.OrderItem;
import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.repository.CustomerRepository;
import com.example.ordermanagement.repository.OrderRepository;
import com.example.ordermanagement.repository.ProductRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Transactional 
    public Order createOrder(Long customerId, List<ItemRequest> itemRequests) {
        // 1. Cari customer di database
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer tidak ditemukan"));

        // 2. Inisialisasi objek Order baru
        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());

        // 3. Proses setiap item yang dibeli
        double total = 0.0;
        for (ItemRequest req : itemRequests) {
            Product product = productRepository.findById(req.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product tidak ditemukan"));

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(req.getQuantity());
            item.setPrice(product.getPrice());
            item.setSubtotal(product.getPrice() * req.getQuantity());
            
            order.addItem(item);
            total += item.getSubtotal();
        }
        
        order.setTotalAmount(total);
        
        // 4. Simpan order ke database
        Order savedOrder = orderRepository.save(order);

        // 5. Kirim pesan ke RabbitMQ agar Inventory mengurangi stok dan Shipping membuat jadwal kirim
        for (ItemRequest req : itemRequests) {
            OrderEvent event = new OrderEvent(savedOrder.getId(), req.getProductId(), req.getQuantity());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
            
            System.out.println("Pesan Event dikirim ke RabbitMQ untuk Order ID: " + savedOrder.getId());
        }

        return savedOrder;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order updateStatus(Long orderId, String status) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    order.setStatus(status);
                    System.out.println("Status Order ID " + orderId + " berhasil diupdate menjadi: " + status);
                    return orderRepository.save(order);
                })
                .orElse(null);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public boolean cancelOrder(Long id) {
        return orderRepository.findById(id).map(order -> {
            // Update status menjadi CANCELLED alih-alih menghapus data
            order.setStatus("CANCELLED");
            orderRepository.save(order);

            for (OrderItem item : order.getItems()) {
                OrderEvent event = new OrderEvent(
                        order.getId(),
                        item.getProduct().getId(),
                        item.getQuantity());

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE,
                        RabbitMQConfig.CANCEL_ROUTING_KEY,
                        event);

                System.out.println("Pesan Cancel dikirim untuk Produk: " + item.getProduct().getName());
            }

            return true;
        }).orElse(false);
    }

    public static class ItemRequest {
        private Long productId;
        private Integer quantity;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}