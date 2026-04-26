package com.demo.productservice.service;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.productservice.dto.CursorPage;
import com.demo.productservice.dto.ProductFilter;
import com.demo.productservice.dto.ProductRequest;
import com.demo.productservice.dto.ProductResponse;

import java.util.List;

public interface ProductService {

    CursorPage<ProductResponse> getProducts(String cursor, int size, ProductFilter filter);

    ProductResponse getProductById(Long id);

    ProductResponse createProduct(ProductRequest productRequest);

    ProductResponse updateProduct(Long id, ProductRequest productRequest);

    void reserveStock(OrderCreatedEvent event);

    void deleteProduct(Long id);
}