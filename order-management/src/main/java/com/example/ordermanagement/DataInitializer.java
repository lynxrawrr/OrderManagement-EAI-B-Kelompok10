package com.example.ordermanagement;

import com.example.ordermanagement.entity.Customer;
import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.repository.CustomerRepository;
import com.example.ordermanagement.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// @Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        // Buat customer
        Customer customer1 = new Customer("John Doe", "john@example.com", "123 Main St");
        customerRepository.save(customer1);

        Customer customer2 = new Customer("Jane Smith", "jane@example.com", "456 Oak Ave");
        customerRepository.save(customer2);

        // Buat products
        Product product1 = new Product("Laptop", 8000000.0, 10);
        Product product2 = new Product("Mouse", 150000.0, 50);
        Product product3 = new Product("Keyboard", 500000.0, 30);

        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);

        System.out.println("Data initialized successfully!");
        System.out.println("Customers: " + customerRepository.count());
        System.out.println("Products: " + productRepository.count());
    }
}