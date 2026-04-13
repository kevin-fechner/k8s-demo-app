package com.demo.productservice.messaging;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ProductService productService;

    @KafkaListener(
            topics = "${kafka.topics.order-events}",
            groupId = "product-service",
            containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    public void onOrderCreated(@Payload OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for orderId={}, items={}",
                event.orderId(), event.items().size());
        productService.reserveStock(event);
    }
}