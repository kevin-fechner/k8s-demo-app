package com.demo.events.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        String customerName,
        String customerEmail,
        List<OrderItem> items,
        BigDecimal totalAmount,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {
    public record OrderItem(
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice
    ) {}
}