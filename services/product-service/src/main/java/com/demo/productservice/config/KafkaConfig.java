package com.demo.productservice.config;

import com.demo.events.order.OrderCreatedEvent;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean(destroyMethod = "close")
    public AdminClient kafkaAdminClient() {
        return AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "3000",
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "5000"
        ));
    }

    // ─── Producer ─────────────────────────────────────────────────────────────

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        props.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ─── Consumer ─────────────────────────────────────────────────────────────

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent>
    orderCreatedKafkaListenerContainerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "product-service");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.demo.events.*");
        props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, OrderCreatedEvent.class.getName());
        props.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        var factory = new ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
        return factory;
    }
}