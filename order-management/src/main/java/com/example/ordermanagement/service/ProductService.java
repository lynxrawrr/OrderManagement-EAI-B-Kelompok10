package com.example.ordermanagement.service;

import com.example.ordermanagement.config.RabbitMQConfig;
import com.example.ordermanagement.dto.OrderEvent;
import com.example.ordermanagement.dto.ProductRequest;
import com.example.ordermanagement.entity.Category;
import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.repository.CategoryRepository;
import com.example.ordermanagement.repository.OrderItemRepository;
import com.example.ordermanagement.repository.ProductRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        // 1. Simpan produk ke database lokal (8080)
        Product saved = productRepository.save(product);
        
        // 2. Kirim pesan sinkronisasi agar Inventory Service (8081) membuat baris stok awal
        OrderEvent syncEvent = new OrderEvent(0L, saved.getId(), saved.getStock());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.PRODUCT_SYNC_ROUTING_KEY, syncEvent);
        System.out.println("Pesan Sinkronisasi Produk dikirim untuk ID: " + saved.getId());
        
        return saved;
    }

    @Transactional
    public Product updateProduct(Long id, ProductRequest request) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setName(request.getName());
                    product.setPrice(request.getPrice());
                    product.setStock(request.getStock());

                    if (request.getCategoryId() != null) {
                        Category category = categoryRepository.findById(request.getCategoryId())
                                .orElseThrow(() -> new IllegalArgumentException("Category tidak ditemukan"));
                        product.setCategory(category);
                    }

                    Product updated = productRepository.save(product);

                    // Sinkronkan total stok terbaru ke Inventory Service.
                    OrderEvent syncEvent = new OrderEvent(0L, updated.getId(), updated.getStock());
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.EXCHANGE,
                            RabbitMQConfig.PRODUCT_SYNC_ROUTING_KEY,
                            syncEvent);
                    System.out.println("Pesan Sinkronisasi Update Produk dikirim untuk ID: " + updated.getId());

                    return updated;
                })
                .orElse(null);
    }

    @Transactional
    public boolean deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            return false;
        }

        if (orderItemRepository.existsByProductId(id)) {
            throw new IllegalStateException("Product sudah dipakai pada order dan tidak boleh dihapus");
        }

        productRepository.deleteById(id);

        OrderEvent deleteEvent = new OrderEvent(0L, id, 0);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.PRODUCT_DELETE_ROUTING_KEY,
                deleteEvent);
        System.out.println("Pesan Penghapusan Produk dikirim untuk ID: " + id);

        return true;
    }
}
