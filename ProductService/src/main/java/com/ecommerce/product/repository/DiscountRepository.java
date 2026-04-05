package com.ecommerce.product.repository;

import com.ecommerce.product.model.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {

    Optional<Discount> findByCode(String code);

    boolean existsByCode(String code);

    List<Discount> findByActiveTrue();

    @Query("SELECT d FROM Discount d WHERE d.active = true AND d.startDate <= :now AND d.endDate >= :now")
    List<Discount> findActiveDiscounts(LocalDateTime now);

    List<Discount> findByProductId(Long productId);

    List<Discount> findByCategoryId(Long categoryId);

    @Query("SELECT d FROM Discount d WHERE d.active = true AND d.startDate <= :now AND d.endDate >= :now " +
           "AND (d.product.id = :productId OR d.category.id = :categoryId OR (d.product IS NULL AND d.category IS NULL))")
    List<Discount> findApplicableDiscounts(Long productId, Long categoryId, LocalDateTime now);
}
