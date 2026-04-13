package com.demo.notificationservice.messaging;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.order.OrderStatusChangedEvent;
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

    @KafkaListener(
            topics = "${kafka.topics.order-events}",
            groupId = "notification-service-created",
            containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    public void onOrderCreated(@Payload OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for orderId={}", event.orderId());
        emailService.sendOrderConfirmation(event);
    }

    @KafkaListener(
            topics = "${kafka.topics.order-events}",
            groupId = "notification-service-status",
            containerFactory = "orderStatusChangedKafkaListenerContainerFactory"
    )
    public void onOrderStatusChanged(@Payload OrderStatusChangedEvent event) {
        log.info("Received OrderStatusChangedEvent for orderId={}, status={}→{}",
                event.orderId(), event.previousStatus(), event.newStatus());
        emailService.sendStatusUpdate(event);
    }
}