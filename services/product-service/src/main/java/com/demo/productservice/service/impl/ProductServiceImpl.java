package com.demo.productservice.service.impl;

import com.demo.events.inventory.StockInsufficientEvent;
import com.demo.events.inventory.StockUpdatedEvent;
import com.demo.events.order.OrderCreatedEvent;
import com.demo.productservice.dto.ProductRequest;
import com.demo.productservice.dto.ProductResponse;
import com.demo.productservice.entity.Product;
import com.demo.productservice.exception.ProductNotFoundException;
import com.demo.productservice.mapper.ProductMapper;
import com.demo.productservice.messaging.InventoryEventPublisher;
import com.demo.productservice.repository.ProductRepository;
import com.demo.productservice.service.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final InventoryEventPublisher inventoryEventPublisher;

    @Override
    public List<ProductResponse> getAllProducts() {
        log.info("Fetching all products");
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with id: {}", id);
        return productRepository.findById(id)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product: {}", request.name());
        Product product = productMapper.toProduct(request);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productMapper.updateEntity(request, product);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    public void reserveStock(OrderCreatedEvent event) {
        for (OrderCreatedEvent.OrderItem item : event.items()) {
            productRepository.findById(item.productId()).ifPresentOrElse(product -> {

                if (product.getStock() >= item.quantity()) {
                    product.setStock(product.getStock() - item.quantity());
                    productRepository.save(product);

                    inventoryEventPublisher.publishStockUpdated(new StockUpdatedEvent(
                            event.orderId(),
                            product.getId(),
                            item.quantity(),
                            product.getStock(),
                            java.time.LocalDateTime.now()
                    ));
                } else {
                    inventoryEventPublisher.publishStockInsufficient(new StockInsufficientEvent(
                            event.orderId(),
                            product.getId(),
                            item.quantity(),
                            product.getStock(),
                            java.time.LocalDateTime.now()
                    ));
                }

            }, () -> log.warn("Product not found: {}", item.productId()));
        }
    }

    @Override
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }
}
