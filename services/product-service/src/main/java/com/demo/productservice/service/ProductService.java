package com.demo.productservice.service;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.productservice.dto.*;

public interface ProductService {

    CursorPage<ProductResponse> getProducts(String cursor, int size, ProductFilter filter);

    ProductResponse getProductById(Long id);

    StockResponse getStockNumbers();

    ProductResponse createProduct(ProductRequest productRequest);

    ProductResponse updateProduct(Long id, ProductRequest productRequest);

    void reserveStock(OrderCreatedEvent event);

    void deleteProduct(Long id);
}