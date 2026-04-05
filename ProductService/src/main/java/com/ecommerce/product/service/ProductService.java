package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductSearchRequest;
import com.ecommerce.product.exception.InsufficientStockException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.model.Category;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockAlertService stockAlertService;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                         StockAlertService stockAlertService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.stockAlertService = stockAlertService;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        logger.debug("Fetching all products");
        return productRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        logger.debug("Fetching product with id: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Product createProduct(Product product) {
        logger.info("Creating new product: {}", product.getName());
        
        if (product.getSku() != null && productRepository.existsBySku(product.getSku())) {
            throw new IllegalArgumentException("Product with SKU '" + product.getSku() + "' already exists");
        }
        
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product productDetails) {
        logger.info("Updating product with id: {}", id);
        Product product = getProductById(id);

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setQuantity(productDetails.getQuantity());
        product.setCategory(productDetails.getCategory());
        product.setSku(productDetails.getSku());
        product.setBrand(productDetails.getBrand());
        product.setLowStockThreshold(productDetails.getLowStockThreshold());

        Product saved = productRepository.save(product);
        
        // Check stock alerts after update
        stockAlertService.checkAndUpdateAlert(id);
        
        return saved;
    }

    public void deleteProduct(Long id) {
        logger.info("Deleting product with id: {}", id);
        Product product = getProductById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    public Product decreaseStock(Long id, Integer quantity) {
        logger.info("Decreasing stock for product {} by {}", id, quantity);
        Product product = getProductById(id);

        if (product.getQuantity() < quantity) {
            logger.error("Insufficient stock for product {}. Available: {}, Requested: {}", 
                    id, product.getQuantity(), quantity);
            throw new InsufficientStockException(id, quantity, product.getQuantity());
        }

        product.setQuantity(product.getQuantity() - quantity);
        Product saved = productRepository.save(product);
        
        // Check stock alerts after decrease
        stockAlertService.checkAndUpdateAlert(id);
        
        return saved;
    }

    public Product increaseStock(Long id, Integer quantity) {
        logger.info("Increasing stock for product {} by {}", id, quantity);
        Product product = getProductById(id);
        product.setQuantity(product.getQuantity() + quantity);
        Product saved = productRepository.save(product);
        
        // Check stock alerts after increase
        stockAlertService.checkAndUpdateAlert(id);
        
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(Long categoryId) {
        logger.debug("Fetching products by category: {}", categoryId);
        return productRepository.findByCategoryId(categoryId);
    }

    @Transactional(readOnly = true)
    public List<Product> searchProductsByName(String name) {
        logger.debug("Searching products by name: {}", name);
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public boolean hasStock(Long id, Integer quantity) {
        Product product = getProductById(id);
        return product.getQuantity() >= quantity;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(ProductSearchRequest request) {
        logger.debug("Searching products with filters");

        Sort sort = Sort.by(
                request.getSortDirection().equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC,
                request.getSortBy()
        );

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<Product> products = productRepository.searchProducts(
                request.getName(),
                request.getCategoryId(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getBrand(),
                pageable
        );

        return products.map(ProductResponse::fromProduct);
    }

    @Transactional(readOnly = true)
    public List<String> getAllBrands() {
        return productRepository.findDistinctBrands();
    }

    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }

    public Product setCategory(Long productId, Long categoryId) {
        Product product = getProductById(productId);
        
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ProductNotFoundException("Category not found: " + categoryId));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }
        
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByBrand(String brand) {
        return productRepository.findByBrand(brand);
    }
}
