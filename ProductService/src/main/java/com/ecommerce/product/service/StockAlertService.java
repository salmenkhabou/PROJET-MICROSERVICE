package com.ecommerce.product.service;

import com.ecommerce.product.dto.StockAlertRequest;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.model.StockAlert;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.StockAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class StockAlertService {

    private static final Logger logger = LoggerFactory.getLogger(StockAlertService.class);

    private final StockAlertRepository stockAlertRepository;
    private final ProductRepository productRepository;

    public StockAlertService(StockAlertRepository stockAlertRepository, ProductRepository productRepository) {
        this.stockAlertRepository = stockAlertRepository;
        this.productRepository = productRepository;
    }

    public StockAlert createAlert(StockAlertRequest request) {
        logger.info("Creating stock alert for product: {}", request.getProductId());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(request.getProductId()));

        // Check if active alert already exists for this product
        if (stockAlertRepository.existsByProductIdAndStatus(request.getProductId(), StockAlert.AlertStatus.ACTIVE)) {
            throw new IllegalArgumentException("Active stock alert already exists for this product");
        }

        StockAlert alert = new StockAlert(product, request.getThreshold(), request.getNotificationEmail());
        
        // Check if already below threshold
        if (product.getQuantity() <= request.getThreshold()) {
            alert.setStatus(StockAlert.AlertStatus.TRIGGERED);
            alert.setTriggeredAt(LocalDateTime.now());
            logger.warn("Stock alert immediately triggered for product {} (stock: {}, threshold: {})",
                    product.getId(), product.getQuantity(), request.getThreshold());
        }

        return stockAlertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public List<StockAlert> getAllAlerts() {
        return stockAlertRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<StockAlert> getTriggeredAlerts() {
        return stockAlertRepository.findTriggeredAlerts();
    }

    @Transactional(readOnly = true)
    public List<StockAlert> getAlertsByProduct(Long productId) {
        return stockAlertRepository.findByProductId(productId);
    }

    @Transactional(readOnly = true)
    public StockAlert getAlertById(Long id) {
        return stockAlertRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Stock alert not found: " + id));
    }

    public StockAlert updateThreshold(Long id, Integer newThreshold) {
        logger.info("Updating stock alert {} threshold to {}", id, newThreshold);

        StockAlert alert = getAlertById(id);
        alert.setThreshold(newThreshold);
        alert.checkAndTrigger();

        return stockAlertRepository.save(alert);
    }

    public void deleteAlert(Long id) {
        logger.info("Deleting stock alert: {}", id);
        stockAlertRepository.deleteById(id);
    }

    public StockAlert resolveAlert(Long id) {
        logger.info("Resolving stock alert: {}", id);

        StockAlert alert = getAlertById(id);
        if (alert.getStatus() != StockAlert.AlertStatus.TRIGGERED) {
            throw new IllegalArgumentException("Alert is not in TRIGGERED status");
        }

        alert.setStatus(StockAlert.AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());

        return stockAlertRepository.save(alert);
    }

    public StockAlert reactivateAlert(Long id) {
        logger.info("Reactivating stock alert: {}", id);

        StockAlert alert = getAlertById(id);
        alert.setStatus(StockAlert.AlertStatus.ACTIVE);
        alert.setTriggeredAt(null);
        alert.setResolvedAt(null);
        alert.checkAndTrigger();

        return stockAlertRepository.save(alert);
    }

    // Check alerts periodically (every 5 minutes)
    @Scheduled(fixedRate = 300000)
    public void checkAllAlerts() {
        logger.debug("Running scheduled stock alert check");

        List<StockAlert> alertsToTrigger = stockAlertRepository.findAlertsToTrigger();
        for (StockAlert alert : alertsToTrigger) {
            alert.setStatus(StockAlert.AlertStatus.TRIGGERED);
            alert.setTriggeredAt(LocalDateTime.now());
            stockAlertRepository.save(alert);

            logger.warn("Stock alert triggered for product {}: {} units remaining (threshold: {})",
                    alert.getProduct().getId(),
                    alert.getProduct().getQuantity(),
                    alert.getThreshold());

            // Here you could send email notification
            sendNotification(alert);
        }
    }

    private void sendNotification(StockAlert alert) {
        // Placeholder for email notification
        if (alert.getNotificationEmail() != null && !alert.getNotificationEmail().isEmpty()) {
            logger.info("Would send notification to {} for low stock on product {}",
                    alert.getNotificationEmail(), alert.getProduct().getName());
            // TODO: Integrate with email service
        }
    }

    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }

    public void checkAndUpdateAlert(Long productId) {
        stockAlertRepository.findByProductIdAndStatus(productId, StockAlert.AlertStatus.ACTIVE)
                .ifPresent(alert -> {
                    alert.checkAndTrigger();
                    stockAlertRepository.save(alert);
                });

        // Also check triggered alerts for resolution
        stockAlertRepository.findByProductIdAndStatus(productId, StockAlert.AlertStatus.TRIGGERED)
                .ifPresent(alert -> {
                    alert.checkAndTrigger();
                    stockAlertRepository.save(alert);
                });
    }
}
