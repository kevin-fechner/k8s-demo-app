package com.demo.events.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record OrderStatusChangedEvent(
        @JsonProperty("orderId") Long orderId,
        @JsonProperty("customerEmail") String customerEmail,
        @JsonProperty("customerName") String customerName,
        @JsonProperty("previousStatus") String previousStatus,
        @JsonProperty("newStatus") String newStatus,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("timestamp") LocalDateTime timestamp
) {
}