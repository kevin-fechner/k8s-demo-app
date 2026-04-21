package com.demo.events.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        @JsonProperty("orderId") Long orderId,
        @JsonProperty("customerName") String customerName,
        @JsonProperty("customerEmail") String customerEmail,
        @JsonProperty("items") List<OrderItem> items,
        @JsonProperty("totalAmount") BigDecimal totalAmount,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("timestamp") LocalDateTime timestamp
) {
    public record OrderItem(
            @JsonProperty("productId") Long productId,
            @JsonProperty("productName") String productName,
            @JsonProperty("quantity") Integer quantity,
            @JsonProperty("unitPrice") BigDecimal unitPrice
    ) {
    }
}