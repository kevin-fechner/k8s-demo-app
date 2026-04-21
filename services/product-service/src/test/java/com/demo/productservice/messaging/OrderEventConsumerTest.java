package com.demo.productservice.messaging;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.productservice.idempotency.IdempotencyService;
import com.demo.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private ProductService productService;
    @Mock
    private IdempotencyService idempotencyService;
    @InjectMocks
    private OrderEventConsumer consumer;

    private OrderCreatedEvent orderCreatedEvent;

    @BeforeEach
    void setUp() {
        orderCreatedEvent = new OrderCreatedEvent(
                1L, "Jane Smith", "jane@example.com",
                List.of(new OrderCreatedEvent.OrderItem(2L, "Widget", 3, new BigDecimal("9.99"))),
                new BigDecimal("29.97"),
                LocalDateTime.of(2024, 1, 15, 9, 0, 0)
        );
    }

    @Test
    @DisplayName("Should delegate OrderCreatedEvent to ProductService.reserveStock")
    void onOrderCreated_DelegatesToReserveStock() {
        when(idempotencyService.tryProcess(anyString(), anyString())).thenReturn(true);

        consumer.onOrderCreated(orderCreatedEvent);

        verify(productService).reserveStock(orderCreatedEvent);
        verifyNoMoreInteractions(productService);
    }

    @Test
    void onOrderCreated_SkipsDuplicate() {
        // Arrange
        when(idempotencyService.tryProcess(anyString(), anyString()))
                .thenReturn(false);

        // Act
        consumer.onOrderCreated(orderCreatedEvent);

        // Assert
        verify(productService, never()).reserveStock(any());
    }
}
