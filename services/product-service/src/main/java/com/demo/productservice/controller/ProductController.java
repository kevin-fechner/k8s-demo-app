package com.demo.productservice.controller;

import com.demo.productservice.dto.CursorPage;
import com.demo.productservice.dto.ProductFilter;
import com.demo.productservice.dto.ProductRequest;
import com.demo.productservice.dto.ProductResponse;
import com.demo.productservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management API")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get products with cursor pagination and filtering")
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    @GetMapping
    public ResponseEntity<CursorPage<ProductResponse>> getProducts(
            @Parameter(description = "Pagination cursor")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "Page size (default 10, max 100)")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by name (partial match)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Minimum price")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Filter by stock availability")
            @RequestParam(required = false) Boolean inStock) {

        // cap size to prevent abuse
        int pageSize = Math.min(size, 100);
        ProductFilter filter = new ProductFilter(name, minPrice, maxPrice, inStock);
        return ResponseEntity.ok(productService.getProducts(cursor, pageSize, filter));
    }

    @Operation(summary = "Get product by ID")
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "Product not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @Operation(summary = "Create a new product")
    @ApiResponse(responseCode = "201", description = "Product created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request));
    }

    @Operation(summary = "Update a product")
    @ApiResponse(responseCode = "200", description = "Product updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Product not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @Operation(summary = "Delete product")
    @ApiResponse(responseCode = "204", description = "Product deleted successfully")
    @ApiResponse(responseCode = "404", description = "Product not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}