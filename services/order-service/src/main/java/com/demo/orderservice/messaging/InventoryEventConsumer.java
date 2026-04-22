package com.demo.orderservice.messaging;

import com.demo.events.inventory.StockInsufficientEvent;
import com.demo.events.inventory.StockUpdatedEvent;
import com.demo.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

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
    public void onStockUpdated(@Payload StockUpdatedEvent event, @Header(value = "eventType", required = false) byte[] eventTypeBytes) {
        String eventType = eventTypeBytes != null
                ? new String(eventTypeBytes, StandardCharsets.UTF_8)
                : null;
        if (!"StockUpdatedEvent".equals(eventType)) {
            log.debug("Skipping message with eventType={}", eventType);
            return;
        }

        log.info("Stock confirmed for orderId={}, product={}, remaining={}",
                event.orderId(), event.productId(), event.remainingStock());
        orderService.confirmOrder(event.orderId());
    }

    @KafkaListener(
            topics = "${kafka.topics.inventory-events}",
            groupId = "order-service-insufficient",
            containerFactory = "stockInsufficientKafkaListenerContainerFactory"
    )
    public void onStockInsufficient(@Payload StockInsufficientEvent event, @Header(value = "eventType", required = false) byte[] eventTypeBytes) {
        String eventType = eventTypeBytes != null
                ? new String(eventTypeBytes, StandardCharsets.UTF_8)
                : null;
        if (!"StockInsufficientEvent".equals(eventType)) {
            log.debug("Skipping message with eventType={}", eventType);
            return;
        }

        log.warn("Stock insufficient for orderId={}, product={}, requested={}, available={}",
                event.orderId(), event.productId(),
                event.requestedQuantity(), event.availableStock());
        orderService.cancelOrder(event.orderId());
    }
}