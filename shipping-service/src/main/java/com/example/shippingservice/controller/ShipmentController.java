package com.example.shippingservice.controller;

import com.example.shippingservice.dto.ShipmentRequest;
import com.example.shippingservice.entity.Shipment;
import com.example.shippingservice.service.ShipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    @Autowired
    private ShipmentService shipmentService;

    // API: Create Shipment via JSON Body
    @PostMapping
    public ResponseEntity<Shipment> createShipment(@RequestBody ShipmentRequest request) {
        return ResponseEntity.ok(shipmentService.createShipment(
                request.getOrderId(),
                request.getProductId(),
                request.getQuantity()));
    }

    // API: Get Shipment (GET /api/shipments/{id})
    @GetMapping("/{id}")
    public ResponseEntity<Shipment> getShipment(@PathVariable Long id) {
        return shipmentService.getShipment(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Shipment>> getAllShipments() {
        return ResponseEntity.ok(shipmentService.getAllShipments());
    }

    // API: Update Status (PUT /api/shipments/{id}/status?status=SHIPPED)
    @PutMapping("/{id}/status")
    public ResponseEntity<Shipment> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(shipmentService.updateShipmentStatus(id, status));
    }
}