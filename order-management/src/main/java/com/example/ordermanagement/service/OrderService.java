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

    // Tambahkan RabbitTemplate untuk mengirim pesan
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Transactional
    public Order createOrder(Long customerId, List<ItemRequest> itemRequests) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer tidak ditemukan"));

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());

        double total = 0.0;
        for (ItemRequest req : itemRequests) {
            Product product = productRepository.findById(req.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product tidak ditemukan"));

            // LOGIKA PEMOTONGAN STOK MANUAL DIHAPUS DARI SINI
            // Kita biarkan Inventory Service yang mengecek dan menahan stok.

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(req.getQuantity());
            item.setPrice(product.getPrice());
            item.setSubtotal(product.getPrice() * req.getQuantity());

            order.addItem(item);
            total += item.getSubtotal();

            // KIRIM PESAN KE RABBITMQ
            OrderEvent event = new OrderEvent(req.getProductId(), req.getQuantity());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);

            System.out.println("Pesan Reserve Stok dikirim ke RabbitMQ untuk Produk ID: " + req.getProductId());
        }

        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order updateStatus(Long orderId, String status) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    order.setStatus(status);
                    return orderRepository.save(order);
                })
                .orElse(null);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    // public boolean deleteOrder(Long id) {
    // if (orderRepository.existsById(id)) {
    // orderRepository.deleteById(id);
    // return true;
    // }
    // return false;
    // }

    public boolean deleteOrder(Long id) {
        return orderRepository.findById(id).map(order -> {
            // Karena Order punya List<OrderItem>, kita loop semua itemnya
            for (OrderItem item : order.getItems()) {
                // Kirim pesan CANCEL untuk setiap produk di dalam order
                OrderEvent event = new OrderEvent(
                        item.getProduct().getId(), // Ambil ID dari relasi Product
                        item.getQuantity());

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE,
                        RabbitMQConfig.CANCEL_ROUTING_KEY,
                        event);

                System.out.println("Pesan Cancel dikirim untuk Produk: " + item.getProduct().getName());
            }

            orderRepository.deleteById(id);
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