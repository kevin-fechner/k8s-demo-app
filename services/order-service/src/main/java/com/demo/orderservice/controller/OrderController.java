package com.demo.orderservice.controller;

import com.demo.orderservice.dto.*;
import com.demo.orderservice.entity.OrderStatus;
import com.demo.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management API")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Get order by ID")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @Operation(summary = "Get orders with cursor pagination and filtering")
    @GetMapping
    public ResponseEntity<CursorPage<OrderResponse>> getOrders(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        int pageSize = Math.min(size, 100);
        OrderFilter filter = new OrderFilter(status, customerEmail, fromDate, toDate);
        return ResponseEntity.ok(orderService.getOrders(cursor, pageSize, filter));
    }

    @Operation(
            summary = "Create a new order",
            description = "Creates an order and publishes an OrderCreatedEvent to Kafka"
    )
    @ApiResponse(responseCode = "201", description = "Order created successfully")
    @ApiResponse(responseCode = "422", description = "Product not available")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request));
    }

    @Operation(summary = "Update order status",
            description = "Updates status and publishes OrderStatusChangedEvent to Kafka")
    @ApiResponse(responseCode = "200", description = "Order status updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }

    @Operation(summary = "Delete order")
    @ApiResponse(responseCode = "204", description = "Order deleted successfully")
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}