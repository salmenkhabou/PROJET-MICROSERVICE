package com.ecommerce.product;

import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        testProduct = new Product("Integration Test Product", new BigDecimal("199.99"), 50, "Test Category");
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllProducts_ReturnsProductList() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", is("Integration Test Product")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getProductById_WhenExists_ReturnsProduct() throws Exception {
        mockMvc.perform(get("/products/{id}", testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Integration Test Product")))
                .andExpect(jsonPath("$.price", is(199.99)))
                .andExpect(jsonPath("$.quantity", is(50)));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getProductById_WhenNotExists_Returns404() throws Exception {
        mockMvc.perform(get("/products/{id}", 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_WithValidData_Returns201() throws Exception {
        Product newProduct = new Product("New Product", new BigDecimal("299.99"), 25, "New Category");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("New Product")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createProduct_WithUserRole_Returns403() throws Exception {
        Product newProduct = new Product("New Product", new BigDecimal("299.99"), 25, "New Category");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_WithInvalidData_Returns400() throws Exception {
        Product invalidProduct = new Product("", null, -1, "");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_WhenExists_ReturnsUpdatedProduct() throws Exception {
        Product updatedProduct = new Product("Updated Product", new BigDecimal("149.99"), 75, "Updated Category");

        mockMvc.perform(put("/products/{id}", testProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Product")))
                .andExpect(jsonPath("$.price", is(149.99)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_WhenExists_Returns204() throws Exception {
        mockMvc.perform(delete("/products/{id}", testProduct.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void decreaseStock_WithSufficientStock_ReturnsUpdatedProduct() throws Exception {
        mockMvc.perform(put("/products/{id}/decrease", testProduct.getId())
                        .param("qty", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", is(40)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void decreaseStock_WithInsufficientStock_Returns400() throws Exception {
        mockMvc.perform(put("/products/{id}/decrease", testProduct.getId())
                        .param("qty", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void checkStock_ReturnsBooleanResult() throws Exception {
        mockMvc.perform(get("/products/{id}/stock", testProduct.getId())
                        .param("qty", "25"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void unauthorizedAccess_Returns401() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isUnauthorized());
    }
}
