package com.demo.orderservice.service.impl;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.order.OrderStatusChangedEvent;
import com.demo.orderservice.client.ProductClient;
import com.demo.orderservice.dto.*;
import com.demo.orderservice.entity.Order;
import com.demo.orderservice.entity.OrderItem;
import com.demo.orderservice.entity.OrderStatus;
import com.demo.orderservice.exception.OrderNotFoundException;
import com.demo.orderservice.exception.ProductNotAvailableException;
import com.demo.orderservice.mapper.OrderMapper;
import com.demo.orderservice.messaging.OrderEventPublisher;
import com.demo.orderservice.repository.OrderRepository;
import com.demo.orderservice.service.OrderService;
import com.demo.orderservice.util.CursorUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;
    private final OrderEventPublisher eventPublisher;
    private final Counter ordersCreatedCounter;
    private final Counter ordersConfirmedCounter;
    private final Counter ordersCancelledCounter;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderMapper orderMapper,
                            ProductClient productClient,
                            OrderEventPublisher eventPublisher,
                            MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.productClient = productClient;
        this.eventPublisher = eventPublisher;

        // Register counters eagerly so they show up in Prometheus from startup
        this.ordersCreatedCounter = Counter.builder("business.orders.placed")
                .description("Total number of orders placed")
                .register(meterRegistry);
        this.ordersConfirmedCounter = Counter.builder("business.orders.confirmed")
                .description("Total number of orders confirmed")
                .register(meterRegistry);
        this.ordersCancelledCounter = Counter.builder("business.orders.cancelled")
                .description("Total number of orders cancelled")
                .register(meterRegistry);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.info("Fetching order with id: {}", id);
        return orderRepository.findByIdWithItems(id)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<OrderResponse> getOrders(
            String cursor, int size, OrderFilter filter) {

        Long cursorId = CursorUtil.decode(cursor);
        Pageable pageable = PageRequest.of(0, size + 1);

        List<Order> orders = orderRepository.findWithCursor(
                cursorId,
                filter.status(),
                filter.customerEmail(),
                filter.fromDate(),
                filter.toDate(),
                pageable
        );

        boolean hasMore = orders.size() > size;
        List<Order> pageData = hasMore ? orders.subList(0, size) : orders;

        String nextCursor = hasMore
                ? CursorUtil.encode(pageData.getLast().getId())
                : null;

        return new CursorPage<>(
                pageData.stream().map(orderMapper::toResponse).toList(),
                nextCursor,
                hasMore,
                pageData.size()
        );
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.customerName());

        Order order = orderMapper.toOrder(request);

        for (OrderItemRequest itemRequest : request.items()) {
            ProductDto product = productClient
                    .getProductById(itemRequest.productId())
                    .orElseThrow(() ->
                            new ProductNotAvailableException(itemRequest.productId()));

            OrderItem item = OrderItem.builder()
                    .productId(product.id())
                    .productName(product.name())
                    .quantity(itemRequest.quantity())
                    .unitPrice(product.price())
                    .build();

            order.addItem(item);
        }

        Order saved = orderRepository.save(order);
        ordersCreatedCounter.increment();

        OrderCreatedEvent event = new OrderCreatedEvent(
                saved.getId(),
                saved.getCustomerName(),
                saved.getCustomerEmail(),
                saved.getItems().stream()
                        .map(item -> new OrderCreatedEvent.OrderItem(
                                item.getProductId(),
                                item.getProductName(),
                                item.getQuantity(),
                                item.getUnitPrice()
                        )).toList(),
                saved.getTotalAmount(),
                LocalDateTime.now()
        );
        log.info("About to publish OrderCreatedEvent for orderId={}", saved.getId());
        eventPublisher.publishOrderCreated(event);
        log.info("Published OrderCreatedEvent for orderId={}", saved.getId());

        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long id, UpdateStatusRequest request) {
        log.info("Updating status of order {} to {}", id, request.status());
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        OrderStatus previousStatus = order.getStatus();
        order.setStatus(request.status());
        Order saved = orderRepository.save(order);

        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                saved.getId(),
                saved.getCustomerEmail(),
                saved.getCustomerName(),
                previousStatus.name(),
                saved.getStatus().name(),
                LocalDateTime.now()
        );
        eventPublisher.publishOrderStatusChanged(event);

        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        String previous = order.getStatus().name();
        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);
        ordersConfirmedCounter.increment();
        eventPublisher.publishOrderStatusChanged(new OrderStatusChangedEvent(
                saved.getId(), saved.getCustomerEmail(), saved.getCustomerName(),
                previous, saved.getStatus().name(), LocalDateTime.now()
        ));
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        String previous = order.getStatus().name();
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        ordersCancelledCounter.increment();
        eventPublisher.publishOrderStatusChanged(new OrderStatusChangedEvent(
                saved.getId(), saved.getCustomerEmail(), saved.getCustomerName(),
                previous, saved.getStatus().name(), LocalDateTime.now()
        ));
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        log.info("Deleting order with id: {}", id);
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException(id);
        }
        orderRepository.deleteById(id);
    }

}