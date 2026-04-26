package com.demo.productservice.dto;

import java.math.BigDecimal;

public record ProductFilter(String name,
                            BigDecimal minPrice,
                            BigDecimal maxPrice,
                            Boolean inStock) {
}
