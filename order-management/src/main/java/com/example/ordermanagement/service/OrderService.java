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
        String normalizedStatus = normalizeStatus(status);
        return orderRepository.findById(orderId)
                .map(order -> {
                    String currentStatus = normalizeStatus(order.getStatus());

                    if (isFinalStatus(currentStatus)) {
                        throw new IllegalStateException("Order dengan status final tidak dapat diubah lagi");
                    }

                    if ("CANCELLED".equals(normalizedStatus)) {
                        throw new IllegalStateException("Gunakan endpoint cancel order untuk membatalkan pesanan");
                    }

                    if ("SHIPPED".equals(normalizedStatus)) {
                        throw new IllegalStateException("Status SHIPPED hanya boleh berasal dari Shipping Service");
                    }

                    if ("FAILED".equals(normalizedStatus)) {
                        throw new IllegalStateException("Status FAILED hanya boleh berasal dari Inventory Service");
                    }

                    if (currentStatus.equals(normalizedStatus)) {
                        return order;
                    }

                    order.setStatus(normalizedStatus);
                    Order updated = orderRepository.save(order);
                    System.out.println("Status Order ID " + orderId + " BERHASIL diupdate menjadi: " + normalizedStatus);
                    return updated;
                })
                .orElseGet(() -> {
                    System.err.println("Gagal update status: Order ID " + orderId + " tidak ditemukan!");
                    return null;
                });
    }

    public Order markAsShippedFromShipping(Long orderId) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    String currentStatus = normalizeStatus(order.getStatus());

                    if ("CANCELLED".equals(currentStatus)) {
                        throw new IllegalStateException("Order yang sudah CANCELLED tidak dapat diubah menjadi SHIPPED");
                    }

                    if ("FAILED".equals(currentStatus)) {
                        throw new IllegalStateException("Order yang sudah FAILED tidak dapat diubah menjadi SHIPPED");
                    }

                    if ("SHIPPED".equals(currentStatus)) {
                        return order;
                    }

                    order.setStatus("SHIPPED");
                    Order updated = orderRepository.save(order);
                    System.out.println("Status Order ID " + orderId + " BERHASIL diupdate menjadi: SHIPPED");
                    return updated;
                })
                .orElseGet(() -> {
                    System.err.println("Gagal update status dari Shipping: Order ID " + orderId + " tidak ditemukan!");
                    return null;
                });
    }

    @Transactional
    public Order markAsFailedFromInventory(Long orderId) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    String currentStatus = normalizeStatus(order.getStatus());

                    if ("FAILED".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {
                        return order;
                    }

                    if ("SHIPPED".equals(currentStatus)) {
                        throw new IllegalStateException("Order yang sudah SHIPPED tidak dapat ditandai FAILED");
                    }

                    order.setStatus("FAILED");
                    Order failedOrder = orderRepository.save(order);
                    publishCancelEvents(order);
                    System.out.println("Status Order ID " + orderId + " BERHASIL diupdate menjadi: FAILED");
                    return failedOrder;
                })
                .orElseGet(() -> {
                    System.err.println("Gagal update status dari Inventory: Order ID " + orderId + " tidak ditemukan!");
                    return null;
                });
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Transactional
    public Optional<Order> cancelOrder(Long id) {
        return orderRepository.findById(id).map(order -> {
            String currentStatus = normalizeStatus(order.getStatus());

            if ("CANCELLED".equals(currentStatus)) {
                throw new IllegalStateException("Order sudah CANCELLED");
            }

            if ("SHIPPED".equals(currentStatus)) {
                throw new IllegalStateException("Order yang sudah SHIPPED tidak dapat dibatalkan");
            }

            if ("FAILED".equals(currentStatus)) {
                throw new IllegalStateException("Order yang sudah FAILED tidak dapat dibatalkan lagi");
            }

            // 1. Update status menjadi CANCELLED
            order.setStatus("CANCELLED");
            Order updatedOrder = orderRepository.save(order);

            // 2. Kirim pesan ke RabbitMQ untuk setiap item agar stok dilepaskan kembali
            publishCancelEvents(order);

            return updatedOrder;
        });
    }

    private void publishCancelEvents(Order order) {
        for (OrderItem item : order.getItems()) {
            OrderEvent event = new OrderEvent(
                    order.getId(),
                    item.getProduct().getId(),
                    item.getQuantity());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.CANCEL_ROUTING_KEY,
                    event);

            System.out.println("Pesan Cancel dikirim untuk Produk: " + item.getProduct().getName() + " (ID: " + item.getProduct().getId() + ") Qty: " + item.getQuantity());
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status wajib diisi");
        }
        return status.trim().toUpperCase();
    }

    private boolean isFinalStatus(String status) {
        return "CANCELLED".equals(status) || "SHIPPED".equals(status) || "FAILED".equals(status);
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
