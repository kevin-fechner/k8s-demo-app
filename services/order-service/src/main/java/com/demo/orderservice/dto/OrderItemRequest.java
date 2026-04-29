package com.demo.orderservice.dto;

import jakarta.validation.constraints.*;

public record OrderItemRequest(
    @NotNull(message = "Product ID is required") Long productId,
    @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity) {}
