package com.demo.orderservice.client;

import com.demo.orderservice.dto.ProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.Optional;

@Component
@Slf4j
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(
            @Value("${clients.product-service.url}") String productServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(productServiceUrl)
                .build();
    }

    public Optional<ProductDto> getProductById(Long productId) {
        try {
            ProductDto product = restClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductDto.class);
            return Optional.ofNullable(product);
        } catch (RestClientException e) {
            log.warn("Could not fetch product with id {}: {}", productId, e.getMessage());
            return Optional.empty();
        }
    }
}