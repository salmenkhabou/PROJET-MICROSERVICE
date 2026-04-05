package com.ecommerce.order.client;

import com.ecommerce.order.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "product-service", fallback = ProductClientFallback.class)
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductDto getProductById(@PathVariable("id") Long id);

    @PutMapping("/products/{id}/decrease")
    ProductDto decreaseStock(@PathVariable("id") Long id, @RequestParam("qty") Integer quantity);

    @GetMapping("/products/{id}/stock")
    Boolean checkStock(@PathVariable("id") Long id, @RequestParam("qty") Integer quantity);
}
