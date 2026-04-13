package com.demo.events.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record OrderStatusChangedEvent(
        Long orderId,
        String customerEmail,
        String customerName,
        String previousStatus,
        String newStatus,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {}