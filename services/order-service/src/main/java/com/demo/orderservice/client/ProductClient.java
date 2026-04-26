package com.demo.orderservice.client;

import com.demo.orderservice.dto.ProductDto;
import com.demo.orderservice.exception.ProductNotAvailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductClient {

    private final RestClient restClient;

    @Value("${clients.product-service.url}")
    private String productServiceUrl;

    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductByIdFallback")
    public Optional<ProductDto> getProductById(Long id) {
        log.debug("Fetching product {} from product-service", id);
        try {
            ProductDto product = restClient.get()
                    .uri(productServiceUrl + "/api/products/{id}", id)
                    .retrieve()
                    .body(ProductDto.class);
            return Optional.ofNullable(product);
        } catch (RestClientException e) {
            log.error("Failed to fetch product {}: {}", id, e.getMessage());
            throw e;
        }
    }

    // Fallback — called when circuit is OPEN or call fails
    private Optional<ProductDto> getProductByIdFallback(Long id, Exception ex) {
        log.warn("Circuit breaker triggered for product-service, productId={}, reason={}",
                id, ex.getMessage());
        throw new ProductNotAvailableException(id);
    }
}