package com.demo.productservice.messaging;

import com.demo.events.inventory.StockInsufficientEvent;
import com.demo.events.inventory.StockUpdatedEvent;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryEventPublisherTest {

    @Mock
    @SuppressWarnings("rawtypes")
    private KafkaTemplate kafkaTemplate;

    @InjectMocks
    private InventoryEventPublisher publisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "inventoryEventsTopic", "inventory-events");
    }

    @Test
    @DisplayName("Should publish StockUpdatedEvent with correct topic, key, and header")
    @SuppressWarnings("unchecked")
    void publishStockUpdated_SendsProducerRecordWithCorrectMetadata() {
        StockUpdatedEvent event = new StockUpdatedEvent(
                1L, 2L, 3, 97,
                LocalDateTime.of(2024, 1, 15, 10, 0, 0)
        );

        publisher.publishStockUpdated(event);

        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<String, Object> producerRecord = captor.getValue();

        assertThat(producerRecord.topic()).isEqualTo("inventory-events");
        assertThat(producerRecord.key()).isEqualTo("1");
        String eventType = new String(producerRecord.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8);
        assertThat(eventType).isEqualTo("StockUpdatedEvent");
    }

    @Test
    @DisplayName("Should publish StockInsufficientEvent with correct topic, key, and header")
    @SuppressWarnings("unchecked")
    void publishStockInsufficient_SendsProducerRecordWithCorrectMetadata() {
        StockInsufficientEvent event = new StockInsufficientEvent(
                2L, 5L, 10, 3,
                LocalDateTime.of(2024, 1, 15, 11, 0, 0)
        );

        publisher.publishStockInsufficient(event);

        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<String, Object> record = captor.getValue();

        assertThat(record.topic()).isEqualTo("inventory-events");
        assertThat(record.key()).isEqualTo("2");
        String eventType = new String(record.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8);
        assertThat(eventType).isEqualTo("StockInsufficientEvent");
    }
}
