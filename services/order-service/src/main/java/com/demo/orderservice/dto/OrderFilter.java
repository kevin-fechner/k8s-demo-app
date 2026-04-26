package com.demo.orderservice.dto;

import com.demo.orderservice.entity.OrderStatus;
import java.time.LocalDateTime;

public record OrderFilter(
        OrderStatus status,
        String customerEmail,
        LocalDateTime fromDate,
        LocalDateTime toDate
) {}