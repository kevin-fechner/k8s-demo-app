package com.demo.orderservice.messaging;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.order.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.order-events}")
    private String orderEventsTopic;

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for orderId={}", event.orderId());
        kafkaTemplate.send(orderEventsTopic, event.orderId().toString(), event);
    }

    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Publishing OrderStatusChangedEvent for orderId={}, status={}→{}",
                event.orderId(), event.previousStatus(), event.newStatus());
        kafkaTemplate.send(orderEventsTopic, event.orderId().toString(), event);
    }
}
