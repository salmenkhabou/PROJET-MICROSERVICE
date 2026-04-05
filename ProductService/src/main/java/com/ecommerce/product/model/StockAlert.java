package com.ecommerce.product.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_alerts")
public class StockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull(message = "Threshold is required")
    @Min(value = 1, message = "Threshold must be at least 1")
    @Column(nullable = false)
    private Integer threshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status = AlertStatus.ACTIVE;

    private LocalDateTime triggeredAt;

    private LocalDateTime resolvedAt;

    private String notificationEmail;

    public StockAlert() {
    }

    public StockAlert(Product product, Integer threshold, String notificationEmail) {
        this.product = product;
        this.threshold = threshold;
        this.notificationEmail = notificationEmail;
    }

    public enum AlertStatus {
        ACTIVE,      // Alert is monitoring
        TRIGGERED,   // Stock fell below threshold
        RESOLVED     // Stock was replenished
    }

    public void checkAndTrigger() {
        if (product.getQuantity() <= threshold && status == AlertStatus.ACTIVE) {
            status = AlertStatus.TRIGGERED;
            triggeredAt = LocalDateTime.now();
        } else if (product.getQuantity() > threshold && status == AlertStatus.TRIGGERED) {
            status = AlertStatus.RESOLVED;
            resolvedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public LocalDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(LocalDateTime triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getNotificationEmail() {
        return notificationEmail;
    }

    public void setNotificationEmail(String notificationEmail) {
        this.notificationEmail = notificationEmail;
    }
}
