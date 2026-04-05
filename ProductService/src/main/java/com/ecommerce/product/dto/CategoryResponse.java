package com.ecommerce.product.dto;

import com.ecommerce.product.model.Category;
import java.util.List;
import java.util.stream.Collectors;

public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Long parentId;
    private String parentName;
    private List<CategoryResponse> subcategories;
    private long productCount;

    public CategoryResponse() {
    }

    public static CategoryResponse fromCategory(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setImageUrl(category.getImageUrl());
        response.setParentId(category.getParentId());
        if (category.getParent() != null) {
            response.setParentName(category.getParent().getName());
        }
        return response;
    }

    public static CategoryResponse fromCategoryWithSubcategories(Category category) {
        CategoryResponse response = fromCategory(category);
        if (category.getSubcategories() != null && !category.getSubcategories().isEmpty()) {
            response.setSubcategories(
                category.getSubcategories().stream()
                    .map(CategoryResponse::fromCategoryWithSubcategories)
                    .collect(Collectors.toList())
            );
        }
        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public List<CategoryResponse> getSubcategories() {
        return subcategories;
    }

    public void setSubcategories(List<CategoryResponse> subcategories) {
        this.subcategories = subcategories;
    }

    public long getProductCount() {
        return productCount;
    }

    public void setProductCount(long productCount) {
        this.productCount = productCount;
    }
}
