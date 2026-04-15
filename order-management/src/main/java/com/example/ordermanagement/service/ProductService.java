package com.example.ordermanagement.service;

import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.entity.Category;
import com.example.ordermanagement.repository.CategoryRepository;
import com.example.ordermanagement.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository; // Tambahkan ini di bagian atas Service

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

public Product updateProduct(Long id, Product request) {
    return productRepository.findById(id)
            .map(product -> {
                product.setName(request.getName());
                product.setPrice(request.getPrice());
                product.setStock(request.getStock());
                
                // LOGIKA EDIT CATEGORY
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