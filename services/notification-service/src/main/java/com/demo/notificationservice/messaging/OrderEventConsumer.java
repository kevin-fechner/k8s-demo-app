package com.demo.notificationservice.messaging;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.order.OrderStatusChangedEvent;
import com.demo.notificationservice.idempotency.IdempotencyService;
import com.demo.notificationservice.service.impl.EmailServiceImpl;
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
public class OrderEventConsumer {

    private final EmailServiceImpl emailServiceImpl;
    private final IdempotencyService idempotencyService;

    @KafkaListener(
            topics = "${kafka.topics.order-events}",
            groupId = "notification-service-created",
            containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    public void onOrderCreated(
            @Payload OrderCreatedEvent event,
            @Header(value = "eventType", required = false) byte[] eventTypeBytes) {
        String eventType = eventTypeBytes != null
                ? new String(eventTypeBytes, StandardCharsets.UTF_8)
                : null;
        if (!"OrderCreatedEvent".equals(eventType)) {
            log.debug("Skipping message with eventType={} in onOrderCreated", eventType);
            return;
        }

        String eventId = "order-created-" + event.orderId();
        if (!idempotencyService.tryProcess(eventId, "OrderCreatedEvent")) {
            return;
        }
        log.info("Received OrderCreatedEvent for orderId={}", event.orderId());
        emailServiceImpl.sendOrderConfirmation(event);
    }

    @KafkaListener(
            topics = "${kafka.topics.order-events}",
            groupId = "notification-service-status",
            containerFactory = "orderStatusChangedKafkaListenerContainerFactory"
    )
    public void onOrderStatusChanged(
            @Payload OrderStatusChangedEvent event,
            @Header(value = "eventType", required = false) byte[] eventTypeBytes) {
        String eventType = eventTypeBytes != null
                ? new String(eventTypeBytes, StandardCharsets.UTF_8)
                : null;
        if (!"OrderStatusChangedEvent".equals(eventType)) {
            log.debug("Skipping message with eventType={} in onOrderStatusChanged", eventType);
            return;
        }

        String eventId = "order-status-" + event.orderId() + "-" + event.newStatus();
        if (!idempotencyService.tryProcess(eventId, "OrderStatusChangedEvent")) {
            return;
        }
        log.info("Received OrderStatusChangedEvent for orderId={}, status={}→{}",
                event.orderId(), event.previousStatus(), event.newStatus());
        emailServiceImpl.sendStatusUpdate(event);
    }
}