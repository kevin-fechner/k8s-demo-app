package com.demo.productservice.controller;

import com.demo.productservice.dto.CursorPage;
import com.demo.productservice.dto.ProductFilter;
import com.demo.productservice.dto.ProductRequest;
import com.demo.productservice.dto.ProductResponse;
import com.demo.productservice.exception.ProductNotFoundException;
import com.demo.productservice.mapper.ProductMapperImpl;
import com.demo.productservice.service.impl.ProductServiceImpl;
import com.demo.productservice.util.CursorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.test.autoconfigure.webmvc.SecurityMockMvcAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProductController.class,
        excludeAutoConfiguration = {
                ServletWebSecurityAutoConfiguration.class,
                SecurityMockMvcAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        }
)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductServiceImpl productService;

    @MockitoBean
    private ProductMapperImpl productMapper;

    private ProductResponse testResponse;
    private ProductRequest testRequest;

    @BeforeEach
    void setUp() {
        testResponse = new ProductResponse(1L,
                "Test Product",
                "Test Description",
                new BigDecimal("99.99"),
                10,
                null,
                null);

        testRequest = new ProductRequest("Test Product",
                "Test Description",
                new BigDecimal("99.99"),
                10);
    }

    @Test
    @DisplayName("GET /api/products - should return cursor page of products")
    void getProducts_Returns200() throws Exception {
        CursorPage<ProductResponse> page = new CursorPage<>(List.of(testResponse), null, false, 1);
        when(productService.getProducts(isNull(), eq(10), any(ProductFilter.class))).thenReturn(page);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Test Product"))
                .andExpect(jsonPath("$.data[0].price").value(99.99))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    @DisplayName("GET /api/products - should forward cursor and filter params to service")
    void getProducts_WithCursorAndFilters_ForwardsParams() throws Exception {
        String cursor = CursorUtil.encode(5L);
        CursorPage<ProductResponse> page = new CursorPage<>(List.of(testResponse), CursorUtil.encode(1L), true, 1);
        when(productService.getProducts(eq(cursor), eq(5), any(ProductFilter.class))).thenReturn(page);

        mockMvc.perform(get("/api/products")
                        .param("cursor", cursor)
                        .param("size", "5")
                        .param("name", "Widget")
                        .param("minPrice", "10.00")
                        .param("maxPrice", "200.00")
                        .param("inStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/products - should cap page size at 100")
    void getProducts_SizeCappedAt100() throws Exception {
        CursorPage<ProductResponse> page = new CursorPage<>(List.of(testResponse), null, false, 1);
        when(productService.getProducts(isNull(), eq(100), any(ProductFilter.class))).thenReturn(page);

        mockMvc.perform(get("/api/products").param("size", "999"))
                .andExpect(status().isOk());

        verify(productService).getProducts(isNull(), eq(100), any(ProductFilter.class));
    }

    @Test
    @DisplayName("GET /api/products/{id} - should return product")
    void getProductById_Returns200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(testResponse);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    @DisplayName("GET /api/products/{id} - should return 404 when not found")
    void getProductById_Returns404() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/products - should create product")
    void createProduct_Returns201() throws Exception {
        when(productService.createProduct(any())).thenReturn(testResponse);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    @DisplayName("POST /api/products - should return 400 for invalid request")
    void createProduct_InvalidRequest_Returns400() throws Exception {
        ProductRequest invalidRequest = new ProductRequest("", "", new BigDecimal(10), 5);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - should delete product")
    void deleteProduct_Returns204() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /api/products/{id} - should update product")
    void updateProduct_Returns200() throws Exception {
        when(productService.updateProduct(eq(1L), any())).thenReturn(testResponse);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    @DisplayName("PUT /api/products/{id} - should return 404 when not found")
    void updateProduct_Returns404() throws Exception {
        when(productService.updateProduct(eq(99L), any()))
                .thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(put("/api/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/products/{id} - should return 400 for invalid request")
    void updateProduct_InvalidRequest_Returns400() throws Exception {
        ProductRequest invalid = new ProductRequest("", "desc", new BigDecimal("10"), 5);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - should return 404 when not found")
    void deleteProduct_Returns404() throws Exception {
        doThrow(new ProductNotFoundException(99L)).when(productService).deleteProduct(99L);

        mockMvc.perform(delete("/api/products/99"))
                .andExpect(status().isNotFound());
    }
}