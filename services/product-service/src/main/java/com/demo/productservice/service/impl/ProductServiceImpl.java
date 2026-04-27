package com.demo.productservice.service.impl;

import com.demo.events.inventory.StockInsufficientEvent;
import com.demo.events.inventory.StockUpdatedEvent;
import com.demo.events.order.OrderCreatedEvent;
import com.demo.productservice.dto.*;
import com.demo.productservice.entity.Product;
import com.demo.productservice.exception.ProductNotFoundException;
import com.demo.productservice.mapper.ProductMapper;
import com.demo.productservice.messaging.InventoryEventPublisher;
import com.demo.productservice.repository.ProductRepository;
import com.demo.productservice.service.ProductService;
import com.demo.productservice.util.CursorUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final InventoryEventPublisher inventoryEventPublisher;
    private final CacheManager cacheManager;
    private final Counter stockReservationsCounter;
    private final Counter stockInsufficientCounter;

    public ProductServiceImpl(ProductRepository productRepository, ProductMapper productMapper, InventoryEventPublisher inventoryEventPublisher, CacheManager cacheManager, MeterRegistry meterRegistry) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.inventoryEventPublisher = inventoryEventPublisher;
        this.cacheManager = cacheManager;
        this.stockReservationsCounter = Counter.builder("business.stock.reserved")
                .description("Total stock reservations")
                .register(meterRegistry);
        this.stockInsufficientCounter = Counter.builder("business.stock.insufficient")
                .description("Total stock insufficient events")
                .register(meterRegistry);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ProductResponse> getProducts(
            String cursor, int size, ProductFilter filter) {

        Long cursorId = CursorUtil.decode(cursor);
        // fetch size+1 to determine if there are more results
        Pageable pageable = PageRequest.of(0, size + 1);

        List<Product> products = productRepository.findWithCursor(
                cursorId,
                filter.name(),
                filter.minPrice(),
                filter.maxPrice(),
                filter.inStock(),
                pageable
        );

        boolean hasMore = products.size() > size;
        List<Product> pageData = hasMore ? products.subList(0, size) : products;

        String nextCursor = hasMore
                ? CursorUtil.encode(pageData.getLast().getId())
                : null;

        return new CursorPage<>(
                pageData.stream().map(productMapper::toResponse).toList(),
                nextCursor,
                hasMore,
                pageData.size()
        );
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with id: {}", id);
        return productRepository.findById(id)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    @Cacheable(value = "stock")
    public StockResponse getStockNumbers() {
        log.info("Fetching stock numbers");
        return productRepository.getStockNumbers();
    }

    @Override
    @CacheEvict(value = "stock")
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product: {}", request.name());
        Product product = productMapper.toProduct(request);
        Product save = productRepository.save(product);
        return productMapper.toResponse(save);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(cacheNames = "stock")
    })
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productMapper.updateEntity(request, product);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void reserveStock(OrderCreatedEvent event) {
        for (OrderCreatedEvent.OrderItem item : event.items()) {
            productRepository.findById(item.productId()).ifPresentOrElse(product -> {

                if (product.getStock() >= item.quantity()) {
                    product.setStock(product.getStock() - item.quantity());
                    productRepository.save(product);
                    var cache = cacheManager.getCache("products");
                    if (cache != null) cache.evict(product.getId());
                    cache = cacheManager.getCache("stock");
                    if (cache != null) cache.evict("");
                    stockReservationsCounter.increment();

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
                    stockInsufficientCounter.increment();
                }

            }, () -> log.warn("Product not found: {}", item.productId()));
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(cacheNames = "stock")
    })
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }

}
