package com.demo.events.inventory;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record StockUpdatedEvent(
        @JsonProperty("orderId") Long orderId,
        @JsonProperty("productId") Long productId,
        @JsonProperty("quantityReserved") Integer quantityReserved,
        @JsonProperty("remainingStock") Integer remainingStock,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("timestamp") LocalDateTime timestamp
) {
}