package com.ecommerce.product.service;

import com.ecommerce.product.dto.DiscountRequest;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.model.Category;
import com.ecommerce.product.model.Discount;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.DiscountRepository;
import com.ecommerce.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DiscountService {

    private static final Logger logger = LoggerFactory.getLogger(DiscountService.class);

    private final DiscountRepository discountRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public DiscountService(DiscountRepository discountRepository, ProductRepository productRepository,
                          CategoryRepository categoryRepository) {
        this.discountRepository = discountRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Discount createDiscount(DiscountRequest request) {
        logger.info("Creating discount: {}", request.getCode());

        if (discountRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Discount code '" + request.getCode() + "' already exists");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        Discount discount = new Discount();
        discount.setCode(request.getCode().toUpperCase());
        discount.setDescription(request.getDescription());
        discount.setType(request.getType());
        discount.setValue(request.getValue());
        discount.setStartDate(request.getStartDate());
        discount.setEndDate(request.getEndDate());
        discount.setMinimumOrderAmount(request.getMinimumOrderAmount() != null ? 
                request.getMinimumOrderAmount() : BigDecimal.ZERO);
        discount.setMaxUses(request.getMaxUses());

        if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(request.getProductId()));
            discount.setProduct(product);
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ProductNotFoundException("Category not found: " + request.getCategoryId()));
            discount.setCategory(category);
        }

        return discountRepository.save(discount);
    }

    @Transactional(readOnly = true)
    public List<Discount> getAllDiscounts() {
        return discountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Discount> getActiveDiscounts() {
        return discountRepository.findActiveDiscounts(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Discount getDiscountById(Long id) {
        return discountRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Discount not found: " + id));
    }

    @Transactional(readOnly = true)
    public Discount getDiscountByCode(String code) {
        return discountRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ProductNotFoundException("Discount not found with code: " + code));
    }

    public Discount validateAndApplyDiscount(String code, BigDecimal orderAmount, Long productId, Long categoryId) {
        logger.info("Validating discount code: {}", code);

        Discount discount = getDiscountByCode(code);

        if (!discount.isValid()) {
            throw new IllegalArgumentException("Discount code is not valid or has expired");
        }

        if (orderAmount.compareTo(discount.getMinimumOrderAmount()) < 0) {
            throw new IllegalArgumentException("Order amount does not meet minimum requirement of " + 
                    discount.getMinimumOrderAmount());
        }

        // Check scope
        if (discount.getProduct() != null && !discount.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("Discount is not applicable to this product");
        }

        if (discount.getCategory() != null && !discount.getCategory().getId().equals(categoryId)) {
            throw new IllegalArgumentException("Discount is not applicable to this category");
        }

        // Increment usage
        discount.setCurrentUses(discount.getCurrentUses() + 1);
        return discountRepository.save(discount);
    }

    public BigDecimal calculateDiscountedPrice(Long productId, String discountCode) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (discountCode == null || discountCode.isEmpty()) {
            return product.getPrice();
        }

        try {
            Discount discount = getDiscountByCode(discountCode);
            if (discount.isValid()) {
                BigDecimal discountAmount = discount.calculateDiscount(product.getPrice());
                return product.getPrice().subtract(discountAmount);
            }
        } catch (Exception e) {
            logger.warn("Invalid discount code: {}", discountCode);
        }

        return product.getPrice();
    }

    @Transactional(readOnly = true)
    public List<Discount> getApplicableDiscounts(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        return discountRepository.findApplicableDiscounts(productId, categoryId, LocalDateTime.now());
    }

    public Discount updateDiscount(Long id, DiscountRequest request) {
        logger.info("Updating discount: {}", id);

        Discount discount = getDiscountById(id);

        discount.setDescription(request.getDescription());
        discount.setType(request.getType());
        discount.setValue(request.getValue());
        discount.setStartDate(request.getStartDate());
        discount.setEndDate(request.getEndDate());
        discount.setMinimumOrderAmount(request.getMinimumOrderAmount());
        discount.setMaxUses(request.getMaxUses());

        return discountRepository.save(discount);
    }

    public void deactivateDiscount(Long id) {
        logger.info("Deactivating discount: {}", id);
        Discount discount = getDiscountById(id);
        discount.setActive(false);
        discountRepository.save(discount);
    }

    public void deleteDiscount(Long id) {
        logger.info("Deleting discount: {}", id);
        discountRepository.deleteById(id);
    }
}
