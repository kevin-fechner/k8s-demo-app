package com.demo.orderservice.service;

import com.demo.orderservice.dto.*;
import java.util.List;

public interface OrderService {
    List<OrderResponse> getAllOrders();
    OrderResponse getOrderById(Long id);
    OrderResponse createOrder(OrderRequest request);
    OrderResponse updateStatus(Long id, UpdateStatusRequest request);
    void confirmOrder(Long orderId);
    void cancelOrder(Long orderId);
    void deleteOrder(Long id);
}