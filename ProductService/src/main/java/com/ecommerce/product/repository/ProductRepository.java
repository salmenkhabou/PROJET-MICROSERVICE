package com.ecommerce.product.repository;

import com.ecommerce.product.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByQuantityGreaterThan(Integer quantity);

    List<Product> findByQuantityLessThanEqual(Integer quantity);

    List<Product> findByActiveTrue();

    List<Product> findByBrand(String brand);

    // Search with filters
    @Query("SELECT p FROM Product p WHERE " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:brand IS NULL OR p.brand = :brand) AND " +
           "p.active = true")
    Page<Product> searchProducts(
            @Param("name") String name,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("brand") String brand,
            Pageable pageable);

    // Find low stock products
    @Query("SELECT p FROM Product p WHERE p.quantity <= p.lowStockThreshold AND p.active = true")
    List<Product> findLowStockProducts();

    // Find products by category including subcategories
    @Query("SELECT p FROM Product p WHERE p.category.id IN :categoryIds AND p.active = true")
    List<Product> findByCategoryIds(@Param("categoryIds") List<Long> categoryIds);

    // Get distinct brands
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL")
    List<String> findDistinctBrands();

    // Count by category
    Long countByCategoryId(Long categoryId);

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);
}

