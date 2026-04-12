package com.demo.productservice.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(@NotBlank(message = "Name is required") @Size(max = 255) String name,
                             String description,
                             @NotNull(message = "Price is required") @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0") BigDecimal price,
                             @NotNull(message = "Stock is required") @Min(value = 0, message = "Stock cannot be negative") Integer stock) {
}
