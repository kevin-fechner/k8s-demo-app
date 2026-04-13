package com.demo.events.inventory;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record StockUpdatedEvent(
        Long orderId,
        Long productId,
        Integer quantityReserved,
        Integer remainingStock,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {}