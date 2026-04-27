package com.demo.productservice.dto;

public record StockResponse(Long total, Long inStock, Long outOfStock) {
}
