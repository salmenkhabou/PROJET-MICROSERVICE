package com.ecommerce.product.controller;

import com.ecommerce.product.dto.DiscountRequest;
import com.ecommerce.product.model.Discount;
import com.ecommerce.product.service.DiscountService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/discounts")
public class DiscountController {

    private static final Logger logger = LoggerFactory.getLogger(DiscountController.class);

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Discount>> getAllDiscounts() {
        logger.info("GET /discounts - Fetching all discounts");
        return ResponseEntity.ok(discountService.getAllDiscounts());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Discount>> getActiveDiscounts() {
        logger.info("GET /discounts/active - Fetching active discounts");
        return ResponseEntity.ok(discountService.getActiveDiscounts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Discount> getDiscountById(@PathVariable Long id) {
        logger.info("GET /discounts/{} - Fetching discount", id);
        return ResponseEntity.ok(discountService.getDiscountById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<Discount> getDiscountByCode(@PathVariable String code) {
        logger.info("GET /discounts/code/{} - Fetching discount by code", code);
        return ResponseEntity.ok(discountService.getDiscountByCode(code));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Discount>> getApplicableDiscounts(@PathVariable Long productId) {
        logger.info("GET /discounts/product/{} - Fetching applicable discounts", productId);
        return ResponseEntity.ok(discountService.getApplicableDiscounts(productId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Discount> createDiscount(@Valid @RequestBody DiscountRequest request) {
        logger.info("POST /discounts - Creating discount: {}", request.getCode());
        Discount discount = discountService.createDiscount(request);
        return new ResponseEntity<>(discount, HttpStatus.CREATED);
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateDiscount(
            @RequestParam String code,
            @RequestParam BigDecimal orderAmount,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long categoryId) {
        
        logger.info("POST /discounts/validate - Validating code: {}", code);
        
        try {
            Discount discount = discountService.validateAndApplyDiscount(code, orderAmount, productId, categoryId);
            BigDecimal discountAmount = discount.calculateDiscount(orderAmount);
            
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "discountCode", discount.getCode(),
                    "discountType", discount.getType(),
                    "discountValue", discount.getValue(),
                    "discountAmount", discountAmount,
                    "finalAmount", orderAmount.subtract(discountAmount)
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/calculate/{productId}")
    public ResponseEntity<Map<String, BigDecimal>> calculateDiscountedPrice(
            @PathVariable Long productId,
            @RequestParam(required = false) String code) {
        
        logger.info("GET /discounts/calculate/{} - Calculating price with code: {}", productId, code);
        BigDecimal discountedPrice = discountService.calculateDiscountedPrice(productId, code);
        
        return ResponseEntity.ok(Map.of("discountedPrice", discountedPrice));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Discount> updateDiscount(@PathVariable Long id, 
                                                   @Valid @RequestBody DiscountRequest request) {
        logger.info("PUT /discounts/{} - Updating discount", id);
        return ResponseEntity.ok(discountService.updateDiscount(id, request));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateDiscount(@PathVariable Long id) {
        logger.info("PUT /discounts/{}/deactivate - Deactivating discount", id);
        discountService.deactivateDiscount(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDiscount(@PathVariable Long id) {
        logger.info("DELETE /discounts/{} - Deleting discount", id);
        discountService.deleteDiscount(id);
        return ResponseEntity.noContent().build();
    }
}
