package com.demo.orderservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should return 404 with message for OrderNotFoundException")
    void handleNotFound_Returns404WithMessage() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleNotFound(new OrderNotFoundException(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).contains("42");
    }

    @Test
    @DisplayName("Should return 422 for ProductNotAvailableException")
    void handleProductNotAvailable_Returns422() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleProductNotAvailable(new ProductNotAvailableException(7L));

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody().status()).isEqualTo(422);
        assertThat(response.getBody().message()).contains("7");
    }

    @Test
    @DisplayName("Should return 400 with validation details for MethodArgumentNotValidException")
    void handleValidation_Returns400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of());

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).startsWith("Validation failed");
    }

    @Test
    @DisplayName("Should return 500 with generic message for unexpected exceptions")
    void handleGeneral_Returns500() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleGeneral(new RuntimeException("something went wrong"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("Internal server error");
    }
}
