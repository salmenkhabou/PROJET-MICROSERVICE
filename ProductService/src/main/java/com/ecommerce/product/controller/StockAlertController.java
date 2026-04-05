package com.ecommerce.product.controller;

import com.ecommerce.product.dto.StockAlertRequest;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.model.StockAlert;
import com.ecommerce.product.service.StockAlertService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock-alerts")
@PreAuthorize("hasRole('ADMIN')")
public class StockAlertController {

    private static final Logger logger = LoggerFactory.getLogger(StockAlertController.class);

    private final StockAlertService stockAlertService;

    public StockAlertController(StockAlertService stockAlertService) {
        this.stockAlertService = stockAlertService;
    }

    @GetMapping
    public ResponseEntity<List<StockAlert>> getAllAlerts() {
        logger.info("GET /stock-alerts - Fetching all alerts");
        return ResponseEntity.ok(stockAlertService.getAllAlerts());
    }

    @GetMapping("/triggered")
    public ResponseEntity<List<StockAlert>> getTriggeredAlerts() {
        logger.info("GET /stock-alerts/triggered - Fetching triggered alerts");
        return ResponseEntity.ok(stockAlertService.getTriggeredAlerts());
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<Product>> getLowStockProducts() {
        logger.info("GET /stock-alerts/low-stock - Fetching low stock products");
        return ResponseEntity.ok(stockAlertService.getLowStockProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockAlert> getAlertById(@PathVariable Long id) {
        logger.info("GET /stock-alerts/{} - Fetching alert", id);
        return ResponseEntity.ok(stockAlertService.getAlertById(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<StockAlert>> getAlertsByProduct(@PathVariable Long productId) {
        logger.info("GET /stock-alerts/product/{} - Fetching alerts for product", productId);
        return ResponseEntity.ok(stockAlertService.getAlertsByProduct(productId));
    }

    @PostMapping
    public ResponseEntity<StockAlert> createAlert(@Valid @RequestBody StockAlertRequest request) {
        logger.info("POST /stock-alerts - Creating alert for product: {}", request.getProductId());
        StockAlert alert = stockAlertService.createAlert(request);
        return new ResponseEntity<>(alert, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/threshold")
    public ResponseEntity<StockAlert> updateThreshold(
            @PathVariable Long id, 
            @RequestParam Integer threshold) {
        logger.info("PUT /stock-alerts/{}/threshold - Updating threshold to {}", id, threshold);
        return ResponseEntity.ok(stockAlertService.updateThreshold(id, threshold));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<StockAlert> resolveAlert(@PathVariable Long id) {
        logger.info("PUT /stock-alerts/{}/resolve - Resolving alert", id);
        return ResponseEntity.ok(stockAlertService.resolveAlert(id));
    }

    @PutMapping("/{id}/reactivate")
    public ResponseEntity<StockAlert> reactivateAlert(@PathVariable Long id) {
        logger.info("PUT /stock-alerts/{}/reactivate - Reactivating alert", id);
        return ResponseEntity.ok(stockAlertService.reactivateAlert(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        logger.info("DELETE /stock-alerts/{} - Deleting alert", id);
        stockAlertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }
}
