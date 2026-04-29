package com.demo.orderservice.messaging;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.order.OrderStatusChangedEvent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
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
    var producerRecord =
        new ProducerRecord<String, Object>(
            orderEventsTopic,
            null,
            event.orderId().toString(),
            event,
            List.of(
                new RecordHeader(
                    "eventType", "OrderCreatedEvent".getBytes(StandardCharsets.UTF_8))));
    kafkaTemplate.send(producerRecord);
  }

  public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
    log.info(
        "Publishing OrderStatusChangedEvent for orderId={}, status={}→{}",
        event.orderId(),
        event.previousStatus(),
        event.newStatus());
    var producerRecord =
        new ProducerRecord<String, Object>(
            orderEventsTopic,
            null,
            event.orderId().toString(),
            event,
            List.of(
                new RecordHeader(
                    "eventType", "OrderStatusChangedEvent".getBytes(StandardCharsets.UTF_8))));
    kafkaTemplate.send(producerRecord);
  }
}
