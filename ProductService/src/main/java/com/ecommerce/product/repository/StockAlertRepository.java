package com.ecommerce.product.repository;

import com.ecommerce.product.model.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {

    List<StockAlert> findByProductId(Long productId);

    Optional<StockAlert> findByProductIdAndStatus(Long productId, StockAlert.AlertStatus status);

    List<StockAlert> findByStatus(StockAlert.AlertStatus status);

    @Query("SELECT sa FROM StockAlert sa WHERE sa.status = 'TRIGGERED'")
    List<StockAlert> findTriggeredAlerts();

    @Query("SELECT sa FROM StockAlert sa JOIN sa.product p WHERE p.quantity <= sa.threshold AND sa.status = 'ACTIVE'")
    List<StockAlert> findAlertsToTrigger();

    boolean existsByProductIdAndStatus(Long productId, StockAlert.AlertStatus status);
}
