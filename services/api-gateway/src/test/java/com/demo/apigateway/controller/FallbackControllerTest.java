package com.demo.apigateway.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(FallbackController.class)
class FallbackControllerTest {

  @Autowired private WebTestClient webTestClient;

  @Test
  @DisplayName("GET /fallback/products - should return 503")
  void productsFallback_Returns503() {
    webTestClient
        .get()
        .uri("/fallback/products")
        .exchange()
        .expectStatus()
        .isEqualTo(503)
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(503)
        .jsonPath("$.message")
        .isEqualTo("Product service is currently unavailable");
  }

  @Test
  @DisplayName("GET /fallback/orders - should return 503")
  void ordersFallback_Returns503() {
    webTestClient
        .get()
        .uri("/fallback/orders")
        .exchange()
        .expectStatus()
        .isEqualTo(503)
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(503)
        .jsonPath("$.message")
        .isEqualTo("Order service is currently unavailable");
  }
}
