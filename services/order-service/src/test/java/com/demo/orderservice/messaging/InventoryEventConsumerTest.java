package com.demo.orderservice.messaging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.demo.events.inventory.StockInsufficientEvent;
import com.demo.events.inventory.StockUpdatedEvent;
import com.demo.orderservice.service.OrderService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryEventConsumerTest {

  @Mock private OrderService orderService;
  @InjectMocks private InventoryEventConsumer consumer;

  private StockUpdatedEvent stockUpdatedEvent;
  private StockInsufficientEvent stockInsufficientEvent;

  @BeforeEach
  void setUp() {
    stockUpdatedEvent =
        new StockUpdatedEvent(1L, 2L, 3, 97, LocalDateTime.of(2024, 1, 15, 10, 0, 0));

    stockInsufficientEvent =
        new StockInsufficientEvent(1L, 2L, 5, 2, LocalDateTime.of(2024, 1, 15, 10, 0, 0));
  }

  @Test
  @DisplayName("Should call confirmOrder when stock is successfully reserved")
  void onStockUpdated_DelegatesToConfirmOrder() {
    consumer.onStockUpdated(
        stockUpdatedEvent, "StockUpdatedEvent".getBytes(StandardCharsets.UTF_8));

    verify(orderService).confirmOrder(1L);
    verifyNoMoreInteractions(orderService);
  }

  @Test
  @DisplayName("Should call cancelOrder when stock is insufficient")
  void onStockInsufficient_DelegatesToCancelOrder() {
    consumer.onStockInsufficient(
        stockInsufficientEvent, "StockInsufficientEvent".getBytes(StandardCharsets.UTF_8));

    verify(orderService).cancelOrder(1L);
    verifyNoMoreInteractions(orderService);
  }
}
