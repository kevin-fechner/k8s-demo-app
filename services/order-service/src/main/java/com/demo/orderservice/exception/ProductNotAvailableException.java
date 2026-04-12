package com.demo.orderservice.exception;

public class ProductNotAvailableException extends RuntimeException {
    public ProductNotAvailableException(Long productId) {
        super("Product not available with id: " + productId);
    }
}