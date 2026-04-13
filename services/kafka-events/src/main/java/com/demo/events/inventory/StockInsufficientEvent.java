package com.demo.events.inventory;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record StockInsufficientEvent(
        Long orderId,
        Long productId,
        Integer requestedQuantity,
        Integer availableStock,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {}