package com.example.inventoryservice.dto; 
import java.io.Serializable;

public class OrderEvent implements Serializable {
    private Long productId;
    private Integer quantity;

    // Default Constructor
    public OrderEvent() {}

    public OrderEvent(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    // Getters and Setters
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}