package com.demo.orderservice.client;

import com.demo.orderservice.dto.ProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductClientTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    private ProductClient productClient;

    @BeforeEach
    void setUp() {
        productClient = new ProductClient("http://localhost");
        ReflectionTestUtils.setField(productClient, "restClient", restClient);
    }

    @Test
    @DisplayName("Should return product wrapped in Optional when server responds successfully")
    void getProductById_Success_ReturnsProduct() {
        ProductDto expected = new ProductDto(1L, "Widget", "A widget", new BigDecimal("9.99"), 100);
        when(restClient.get()
                .uri(anyString(), any(Long.class))
                .retrieve()
                .body(ProductDto.class))
                .thenReturn(expected);

        Optional<ProductDto> result = productClient.getProductById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1L);
        assertThat(result.get().name()).isEqualTo("Widget");
    }

    @Test
    @DisplayName("Should return empty Optional when server returns null body")
    void getProductById_NullResponse_ReturnsEmpty() {
        when(restClient.get()
                .uri(anyString(), any(Long.class))
                .retrieve()
                .body(ProductDto.class))
                .thenReturn(null);

        Optional<ProductDto> result = productClient.getProductById(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty Optional and log warning when RestClientException is thrown")
    void getProductById_RestClientException_ReturnsEmpty() {
        when(restClient.get()
                .uri(anyString(), any(Long.class))
                .retrieve()
                .body(ProductDto.class))
                .thenThrow(new RestClientException("Connection refused"));

        Optional<ProductDto> result = productClient.getProductById(1L);

        assertThat(result).isEmpty();
    }
}
