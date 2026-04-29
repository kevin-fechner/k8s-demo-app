package com.demo.orderservice.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.order.OrderStatusChangedEvent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

  @Mock
  @SuppressWarnings("rawtypes")
  private KafkaTemplate kafkaTemplate;

  @InjectMocks private OrderEventPublisher publisher;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(publisher, "orderEventsTopic", "order-events");
  }

  @Test
  @DisplayName("Should publish OrderCreatedEvent with correct topic, key, and header")
  @SuppressWarnings("unchecked")
  void publishOrderCreated_SendsProducerRecordWithCorrectMetadata() {
    OrderCreatedEvent event =
        new OrderCreatedEvent(
            1L,
            "John Doe",
            "john@example.com",
            List.of(new OrderCreatedEvent.OrderItem(1L, "Widget", 2, new BigDecimal("9.99"))),
            new BigDecimal("19.98"),
            LocalDateTime.of(2024, 1, 15, 10, 0, 0));

    publisher.publishOrderCreated(event);

    ArgumentCaptor<ProducerRecord<String, Object>> captor =
        ArgumentCaptor.forClass(ProducerRecord.class);
    verify(kafkaTemplate).send(captor.capture());
    ProducerRecord<String, Object> producerRecord = captor.getValue();

    assertThat(producerRecord.topic()).isEqualTo("order-events");
    assertThat(producerRecord.key()).isEqualTo("1");
    String eventType =
        new String(
            producerRecord.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8);
    assertThat(eventType).isEqualTo("OrderCreatedEvent");
  }

  @Test
  @DisplayName("Should publish OrderStatusChangedEvent with correct topic, key, and header")
  @SuppressWarnings("unchecked")
  void publishOrderStatusChanged_SendsProducerRecordWithCorrectMetadata() {
    OrderStatusChangedEvent event =
        new OrderStatusChangedEvent(
            2L,
            "john@example.com",
            "John Doe",
            "PENDING",
            "CONFIRMED",
            LocalDateTime.of(2024, 1, 15, 11, 0, 0));

    publisher.publishOrderStatusChanged(event);

    ArgumentCaptor<ProducerRecord<String, Object>> captor =
        ArgumentCaptor.forClass(ProducerRecord.class);
    verify(kafkaTemplate).send(captor.capture());
    ProducerRecord<String, Object> producerRecord = captor.getValue();

    assertThat(producerRecord.topic()).isEqualTo("order-events");
    assertThat(producerRecord.key()).isEqualTo("2");
    String eventType =
        new String(
            producerRecord.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8);
    assertThat(eventType).isEqualTo("OrderStatusChangedEvent");
  }
}
