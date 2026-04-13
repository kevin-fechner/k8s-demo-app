package com.demo.orderservice.messaging;

import com.demo.events.inventory.StockInsufficientEvent;
import com.demo.events.inventory.StockUpdatedEvent;
import com.demo.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${kafka.topics.inventory-events}",
            groupId = "order-service",
            containerFactory = "stockUpdatedKafkaListenerContainerFactory"
    )
    public void onStockUpdated(@Payload StockUpdatedEvent event) {
        log.info("Stock confirmed for orderId={}, product={}, remaining={}",
                event.orderId(), event.productId(), event.remainingStock());
        orderService.confirmOrder(event.orderId());
    }

    @KafkaListener(
            topics = "${kafka.topics.inventory-events}",
            groupId = "order-service-insufficient",
            containerFactory = "stockInsufficientKafkaListenerContainerFactory"
    )
    public void onStockInsufficient(@Payload StockInsufficientEvent event) {
        log.warn("Stock insufficient for orderId={}, product={}, requested={}, available={}",
                event.orderId(), event.productId(),
                event.requestedQuantity(), event.availableStock());
        orderService.cancelOrder(event.orderId());
    }
}