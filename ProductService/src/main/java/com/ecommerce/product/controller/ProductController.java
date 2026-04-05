package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductSearchRequest;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.service.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        logger.info("GET /products - Fetching all products");
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        logger.info("GET /products/{} - Fetching product by id", id);
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        logger.info("POST /products - Creating new product: {}", product.getName());
        Product createdProduct = productService.createProduct(product);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody Product product) {
        logger.info("PUT /products/{} - Updating product", id);
        Product updatedProduct = productService.updateProduct(id, product);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        logger.info("DELETE /products/{} - Deleting product", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/decrease")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> decreaseStock(@PathVariable Long id, @RequestParam("qty") Integer quantity) {
        logger.info("PUT /products/{}/decrease?qty={} - Decreasing stock", id, quantity);
        Product updatedProduct = productService.decreaseStock(id, quantity);
        return ResponseEntity.ok(updatedProduct);
    }

    @PutMapping("/{id}/increase")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> increaseStock(@PathVariable Long id, @RequestParam("qty") Integer quantity) {
        logger.info("PUT /products/{}/increase?qty={} - Increasing stock", id, quantity);
        Product updatedProduct = productService.increaseStock(id, quantity);
        return ResponseEntity.ok(updatedProduct);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable Long categoryId) {
        logger.info("GET /products/category/{} - Fetching products by category", categoryId);
        List<Product> products = productService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam("name") String name) {
        logger.info("GET /products/search?name={} - Searching products", name);
        List<Product> products = productService.searchProductsByName(name);
        return ResponseEntity.ok(products);
    }

    @PostMapping("/search/advanced")
    public ResponseEntity<Page<ProductResponse>> advancedSearch(@RequestBody ProductSearchRequest request) {
        logger.info("POST /products/search/advanced - Advanced search");
        Page<ProductResponse> products = productService.searchProducts(request);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<Boolean> checkStock(@PathVariable Long id, @RequestParam("qty") Integer quantity) {
        logger.info("GET /products/{}/stock?qty={} - Checking stock availability", id, quantity);
        boolean hasStock = productService.hasStock(id, quantity);
        return ResponseEntity.ok(hasStock);
    }

    @GetMapping("/brands")
    public ResponseEntity<List<String>> getAllBrands() {
        logger.info("GET /products/brands - Fetching all brands");
        return ResponseEntity.ok(productService.getAllBrands());
    }

    @GetMapping("/brand/{brand}")
    public ResponseEntity<List<Product>> getProductsByBrand(@PathVariable String brand) {
        logger.info("GET /products/brand/{} - Fetching products by brand", brand);
        return ResponseEntity.ok(productService.getProductsByBrand(brand));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Product>> getLowStockProducts() {
        logger.info("GET /products/low-stock - Fetching low stock products");
        return ResponseEntity.ok(productService.getLowStockProducts());
    }

    @PutMapping("/{productId}/category/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> setProductCategory(
            @PathVariable Long productId, 
            @PathVariable Long categoryId) {
        logger.info("PUT /products/{}/category/{} - Setting product category", productId, categoryId);
        return ResponseEntity.ok(productService.setCategory(productId, categoryId));
    }
}
