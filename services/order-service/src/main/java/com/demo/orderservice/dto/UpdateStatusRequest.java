package com.demo.orderservice.dto;

import com.demo.orderservice.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull(message = "Status is required") OrderStatus status) {}
