package com.demo.events.inventory;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record StockInsufficientEvent(
        @JsonProperty("orderId") Long orderId,
        @JsonProperty("productId") Long productId,
        @JsonProperty("requestedQuantity") Integer requestedQuantity,
        @JsonProperty("availableStock") Integer availableStock,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("timestamp") LocalDateTime timestamp
) {
}