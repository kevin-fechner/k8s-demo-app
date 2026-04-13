package com.demo.productservice.messaging;

import com.demo.events.inventory.StockInsufficientEvent;
import com.demo.events.inventory.StockUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.inventory-events}")
    private String inventoryEventsTopic;

    public void publishStockUpdated(StockUpdatedEvent event) {
        log.info("Publishing StockUpdatedEvent for orderId={}, productId={}, remaining={}",
                event.orderId(), event.productId(), event.remainingStock());
        kafkaTemplate.send(inventoryEventsTopic, event.orderId().toString(), event);
    }

    public void publishStockInsufficient(StockInsufficientEvent event) {
        log.warn("Publishing StockInsufficientEvent for orderId={}, productId={}, requested={}, available={}",
                event.orderId(), event.productId(),
                event.requestedQuantity(), event.availableStock());
        kafkaTemplate.send(inventoryEventsTopic, event.orderId().toString(), event);
    }
}