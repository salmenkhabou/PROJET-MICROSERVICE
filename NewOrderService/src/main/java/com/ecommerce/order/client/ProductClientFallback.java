package com.ecommerce.order.client;

import com.ecommerce.order.dto.ProductDto;
import com.ecommerce.order.exception.ProductServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductClientFallback implements ProductClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductClientFallback.class);

    @Override
    public ProductDto getProductById(Long id) {
        logger.error("Fallback: Product service unavailable when fetching product {}", id);
        throw new ProductServiceUnavailableException("Unable to fetch product " + id + ". Product service is unavailable.");
    }

    @Override
    public ProductDto decreaseStock(Long id, Integer quantity) {
        logger.error("Fallback: Product service unavailable when decreasing stock for product {}", id);
        throw new ProductServiceUnavailableException("Unable to decrease stock for product " + id + ". Product service is unavailable.");
    }

    @Override
    public Boolean checkStock(Long id, Integer quantity) {
        logger.error("Fallback: Product service unavailable when checking stock for product {}", id);
        throw new ProductServiceUnavailableException("Unable to check stock for product " + id + ". Product service is unavailable.");
    }
}
