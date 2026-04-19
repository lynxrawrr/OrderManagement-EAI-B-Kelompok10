package com.example.shippingservice.service;

import com.example.shippingservice.dto.OrderEvent;
import com.example.shippingservice.entity.Shipment;
import com.example.shippingservice.repository.ShipmentRepository;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;

@Service
public class ShipmentService {

    @Autowired
    private ShipmentRepository shipmentRepository;

    // Metode untuk membuat jadwal pengiriman baru 
    public Shipment createShipment(Long orderId, Long productId, Integer quantity) {
        Shipment shipment = new Shipment();
        shipment.setOrderId(orderId);
        shipment.setProductId(productId);
        shipment.setQuantity(quantity);
        shipment.setStatus("PENDING");
        shipment.setTrackingNumber("TRK-" + System.currentTimeMillis()); 
        return shipmentRepository.save(shipment);
    }

    // 2. Get Shipment
    public Optional<Shipment> getShipment(Long id) {
        return shipmentRepository.findById(id);
    }

    // 3. Update Shipment Status
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public Shipment updateShipmentStatus(Long id, String status) {
        return shipmentRepository.findById(id).map(shipment -> {
            shipment.setStatus(status);
            Shipment updated = shipmentRepository.save(shipment);

            // Jika kurir mengubah status jadi SHIPPED, kirim pesan ke Inventory agar stok cadangan dibersihkan
            if ("SHIPPED".equalsIgnoreCase(status)) {
                OrderEvent finalizeEvent = new OrderEvent(updated.getOrderId(), updated.getProductId(), updated.getQuantity());
                rabbitTemplate.convertAndSend("order_exchange", "order_shipped_key", finalizeEvent);
                System.out.println("Pesanan DIKIRIM! Mengirim pesan finalize stok ke Inventory...");
            }

            return updated;
        }).orElseThrow(() -> new RuntimeException("Pengiriman tidak ditemukan"));
    }

    public List<Shipment> getAllShipments() {
        return shipmentRepository.findAll();
    }

    // Metode untuk membatalkan pengiriman (jika pesanan dibatalkan)
    public void cancelShipment(Long orderId) {
        List<Shipment> shipments = shipmentRepository.findByOrderId(orderId);
        for (Shipment shipment : shipments) {
            // Hanya batalkan jika status masih PENDING atau belum dikirim
            if ("PENDING".equalsIgnoreCase(shipment.getStatus())) {
                shipment.setStatus("CANCELLED");
                shipmentRepository.save(shipment);
                System.out.println("Shipment ID " + shipment.getId() + " untuk Order ID " + orderId + " telah dibatalkan.");
            }
        }
    }
}