package com.ecommerce.product.controller;

import com.ecommerce.product.model.ProductImage;
import com.ecommerce.product.service.ProductImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/products/{productId}/images")
public class ProductImageController {

    private static final Logger logger = LoggerFactory.getLogger(ProductImageController.class);

    private final ProductImageService imageService;

    public ProductImageController(ProductImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping
    public ResponseEntity<List<ProductImage>> getProductImages(@PathVariable Long productId) {
        logger.info("GET /products/{}/images - Fetching images", productId);
        return ResponseEntity.ok(imageService.getProductImages(productId));
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long productId, @PathVariable Long imageId) {
        logger.info("GET /products/{}/images/{} - Fetching image", productId, imageId);
        
        ProductImage image = imageService.getImage(imageId);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getFileName() + "\"")
                .body(image.getData());
    }

    @GetMapping("/primary")
    public ResponseEntity<byte[]> getPrimaryImage(@PathVariable Long productId) {
        logger.info("GET /products/{}/images/primary - Fetching primary image", productId);
        
        ProductImage image = imageService.getPrimaryImage(productId);
        
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getFileName() + "\"")
                .body(image.getData());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductImage> uploadImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "primary", defaultValue = "false") boolean isPrimary) throws IOException {
        
        logger.info("POST /products/{}/images - Uploading image", productId);
        ProductImage image = imageService.uploadImage(productId, file, isPrimary);
        return new ResponseEntity<>(image, HttpStatus.CREATED);
    }

    @PutMapping("/{imageId}/primary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductImage> setPrimaryImage(
            @PathVariable Long productId, 
            @PathVariable Long imageId) {
        
        logger.info("PUT /products/{}/images/{}/primary - Setting primary image", productId, imageId);
        return ResponseEntity.ok(imageService.setPrimaryImage(productId, imageId));
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteImage(@PathVariable Long productId, @PathVariable Long imageId) {
        logger.info("DELETE /products/{}/images/{} - Deleting image", productId, imageId);
        imageService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }
}
