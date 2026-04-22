package com.demo.productservice.messaging;

import com.demo.events.inventory.StockInsufficientEvent;
import com.demo.events.inventory.StockUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
        var record = new ProducerRecord<String, Object>(
                inventoryEventsTopic,
                null,
                event.orderId().toString(),
                event,
                List.of(new RecordHeader("eventType", "StockUpdatedEvent".getBytes(StandardCharsets.UTF_8)))
        );

        kafkaTemplate.send(record);
    }

    public void publishStockInsufficient(StockInsufficientEvent event) {
        log.warn("Publishing StockInsufficientEvent for orderId={}, productId={}, requested={}, available={}",
                event.orderId(), event.productId(),
                event.requestedQuantity(), event.availableStock());
        var record = new ProducerRecord<String, Object>(
                inventoryEventsTopic,
                null,
                event.orderId().toString(),
                event,
                List.of(new RecordHeader("eventType", "StockInsufficientEvent".getBytes(StandardCharsets.UTF_8)))
        );
        kafkaTemplate.send(record);
    }
}