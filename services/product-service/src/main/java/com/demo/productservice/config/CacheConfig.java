package com.demo.productservice.config;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheManagerCustomizer() {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.demo.productservice.dto.")
                .allowIfSubType("java.")
                .build();
        ObjectMapper mapper = JsonMapper.builder()
                .activateDefaultTypingAsProperty(ptv, DefaultTyping.NON_FINAL, "@class")
                .build();
        GenericJacksonJsonRedisSerializer serializer = new GenericJacksonJsonRedisSerializer(mapper);
        return builder -> builder
                .transactionAware()
                .cacheDefaults(
                        builder.cacheDefaults()
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                        .fromSerializer(serializer)));
    }
}
