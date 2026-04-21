package com.demo.notificationservice.messaging;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.order.OrderStatusChangedEvent;
import com.demo.notificationservice.idempotency.IdempotencyService;
import com.demo.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final EmailService emailService;
    private final IdempotencyService idempotencyService;

    @KafkaListener(
            topics = "${kafka.topics.order-events}",
            groupId = "notification-service-created",
            containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    public void onOrderCreated(@Payload OrderCreatedEvent event) {
        String eventId = "order-created-" + event.orderId();
        if (!idempotencyService.tryProcess(eventId, "OrderCreatedEvent")) {
            return;
        }
        log.info("Received OrderCreatedEvent for orderId={}", event.orderId());
        emailService.sendOrderConfirmation(event);
    }

    @KafkaListener(
            topics = "${kafka.topics.order-events}",
            groupId = "notification-service-status",
            containerFactory = "orderStatusChangedKafkaListenerContainerFactory"
    )
    public void onOrderStatusChanged(@Payload OrderStatusChangedEvent event) {
        String eventId = "order-status-" + event.orderId() + "-" + event.newStatus();
        if (!idempotencyService.tryProcess(eventId, "OrderStatusChangedEvent")) {
            return;
        }
        log.info("Received OrderStatusChangedEvent for orderId={}, status={}→{}",
                event.orderId(), event.previousStatus(), event.newStatus());
        emailService.sendStatusUpdate(event);
    }
}