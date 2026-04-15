package com.example.ordermanagement.service;

import com.example.ordermanagement.config.RabbitMQConfig;
import com.example.ordermanagement.dto.OrderEvent;
import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.entity.Category;
import com.example.ordermanagement.repository.CategoryRepository;
import com.example.ordermanagement.repository.ProductRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository; 

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

public Product updateProduct(Long id, Product request) {
    return productRepository.findById(id)
            .map(product -> {
                // Update data dasar produk
                product.setName(request.getName());
                product.setPrice(request.getPrice());
                product.setStock(request.getStock());
                
                // Cari dan pasangkan kategori jika ada perubahan
                if (request.getCategory() != null && request.getCategory().getId() != null) {
                    Category category = categoryRepository.findById(request.getCategory().getId())
                            .orElseThrow(() -> new RuntimeException("Category tidak ditemukan"));
                    product.setCategory(category);
                }
                
                return productRepository.save(product);
            })
            .orElse(null);
}

    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }
}