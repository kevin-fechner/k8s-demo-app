package com.demo.orderservice.service.impl;

import com.demo.orderservice.client.ProductClient;
import com.demo.orderservice.dto.*;
import com.demo.orderservice.entity.*;
import com.demo.orderservice.exception.*;
import com.demo.orderservice.mapper.OrderMapper;
import com.demo.orderservice.repository.OrderRepository;
import com.demo.orderservice.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;

    @Override
    public List<OrderResponse> getAllOrders() {
        log.info("Fetching all orders");
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        log.info("Fetching order with id: {}", id);
        return orderRepository.findByIdWithItems(id)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
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

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    public OrderResponse updateStatus(Long id, UpdateStatusRequest request) {
        log.info("Updating status of order {} to {}", id, request.status());
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        order.setStatus(request.status());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    public void deleteOrder(Long id) {
        log.info("Deleting order with id: {}", id);
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException(id);
        }
        orderRepository.deleteById(id);
    }
}