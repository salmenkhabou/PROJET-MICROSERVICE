package com.ecommerce.product.service;

import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.model.ProductImage;
import com.ecommerce.product.repository.ProductImageRepository;
import com.ecommerce.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Transactional
public class ProductImageService {

    private static final Logger logger = LoggerFactory.getLogger(ProductImageService.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;

    public ProductImageService(ProductImageRepository imageRepository, ProductRepository productRepository) {
        this.imageRepository = imageRepository;
        this.productRepository = productRepository;
    }

    public ProductImage uploadImage(Long productId, MultipartFile file, boolean isPrimary) throws IOException {
        logger.info("Uploading image for product: {}", productId);

        validateFile(file);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        // If this is set as primary, unset any existing primary
        if (isPrimary) {
            imageRepository.findByProductIdAndIsPrimaryTrue(productId)
                    .ifPresent(img -> {
                        img.setPrimary(false);
                        imageRepository.save(img);
                    });
        }

        ProductImage image = new ProductImage(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes(),
                product
        );
        image.setPrimary(isPrimary);
        image.setImageUrl("/products/" + productId + "/images/" + image.getId());

        image = imageRepository.save(image);
        image.setImageUrl("/products/" + productId + "/images/" + image.getId());
        image = imageRepository.save(image);

        logger.info("Image uploaded successfully with id: {}", image.getId());
        return image;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 5MB");
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: JPEG, PNG, GIF, WebP");
        }
    }

    @Transactional(readOnly = true)
    public List<ProductImage> getProductImages(Long productId) {
        return imageRepository.findByProductId(productId);
    }

    @Transactional(readOnly = true)
    public ProductImage getImage(Long imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new ProductNotFoundException("Image not found: " + imageId));
    }

    @Transactional(readOnly = true)
    public ProductImage getPrimaryImage(Long productId) {
        return imageRepository.findByProductIdAndIsPrimaryTrue(productId)
                .orElse(null);
    }

    public void deleteImage(Long imageId) {
        logger.info("Deleting image: {}", imageId);
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ProductNotFoundException("Image not found: " + imageId));
        imageRepository.delete(image);
    }

    public void deleteAllProductImages(Long productId) {
        logger.info("Deleting all images for product: {}", productId);
        imageRepository.deleteByProductId(productId);
    }

    public ProductImage setPrimaryImage(Long productId, Long imageId) {
        logger.info("Setting primary image {} for product {}", imageId, productId);

        // Unset current primary
        imageRepository.findByProductIdAndIsPrimaryTrue(productId)
                .ifPresent(img -> {
                    img.setPrimary(false);
                    imageRepository.save(img);
                });

        // Set new primary
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ProductNotFoundException("Image not found: " + imageId));

        if (!image.getProductId().equals(productId)) {
            throw new IllegalArgumentException("Image does not belong to this product");
        }

        image.setPrimary(true);
        return imageRepository.save(image);
    }
}
