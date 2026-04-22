package com.demo.notificationservice.messaging;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.order.OrderStatusChangedEvent;
import com.demo.notificationservice.idempotency.IdempotencyService;
import com.demo.notificationservice.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private EmailService emailService;
    @Mock
    private IdempotencyService idempotencyService;
    @InjectMocks
    private OrderEventConsumer consumer;

    private OrderCreatedEvent orderCreatedEvent;
    private OrderStatusChangedEvent orderStatusChangedEvent;

    @BeforeEach
    void setUp() {
        orderCreatedEvent = new OrderCreatedEvent(
                1L, "Jane Smith", "jane@example.com",
                List.of(new OrderCreatedEvent.OrderItem(2L, "Gadget", 1, new BigDecimal("49.99"))),
                new BigDecimal("49.99"),
                LocalDateTime.of(2024, 1, 15, 9, 0, 0)
        );

        orderStatusChangedEvent = new OrderStatusChangedEvent(
                1L, "jane@example.com", "Jane Smith",
                "CONFIRMED", "SHIPPED",
                LocalDateTime.of(2024, 1, 16, 12, 0, 0)
        );
    }

    @Test
    @DisplayName("Should delegate OrderCreatedEvent to EmailService.sendOrderConfirmation")
    void onOrderCreated_DelegatesToEmailService() {
        when(idempotencyService.tryProcess(anyString(), anyString())).thenReturn(true);

        consumer.onOrderCreated(orderCreatedEvent, "OrderCreatedEvent".getBytes(StandardCharsets.UTF_8));

        verify(emailService).sendOrderConfirmation(orderCreatedEvent);
        verifyNoMoreInteractions(emailService);
    }

    @Test
    @DisplayName("Should skip OrderCreatedEvent when idempotency check fails")
    void onOrderCreated_SkipsWhenAlreadyProcessed() {
        when(idempotencyService.tryProcess(anyString(), anyString())).thenReturn(false);

        consumer.onOrderCreated(orderCreatedEvent, "OrderCreatedEvent".getBytes(StandardCharsets.UTF_8));

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should skip OrderCreatedEvent when eventType header does not match")
    void onOrderCreated_SkipsWrongEventType() {
        consumer.onOrderCreated(orderCreatedEvent, "OrderStatusChangedEvent".getBytes(StandardCharsets.UTF_8));

        verifyNoInteractions(idempotencyService);
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should delegate OrderStatusChangedEvent to EmailService.sendStatusUpdate")
    void onOrderStatusChanged_DelegatesToEmailService() {
        when(idempotencyService.tryProcess(anyString(), anyString())).thenReturn(true);

        consumer.onOrderStatusChanged(orderStatusChangedEvent, "OrderStatusChangedEvent".getBytes(StandardCharsets.UTF_8));

        verify(emailService).sendStatusUpdate(orderStatusChangedEvent);
        verifyNoMoreInteractions(emailService);
    }

    @Test
    @DisplayName("Should skip OrderStatusChangedEvent when idempotency check fails")
    void onOrderStatusChanged_SkipsWhenAlreadyProcessed() {
        when(idempotencyService.tryProcess(anyString(), anyString())).thenReturn(false);

        consumer.onOrderStatusChanged(orderStatusChangedEvent, "OrderStatusChangedEvent".getBytes(StandardCharsets.UTF_8));

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should skip OrderStatusChangedEvent when eventType header does not match")
    void onOrderStatusChanged_SkipsWrongEventType() {
        consumer.onOrderStatusChanged(orderStatusChangedEvent, "OrderCreatedEvent".getBytes(StandardCharsets.UTF_8));

        verifyNoInteractions(idempotencyService);
        verifyNoInteractions(emailService);
    }
}
