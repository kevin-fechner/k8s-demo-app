package com.demo.orderservice.service;

import com.demo.orderservice.dto.*;

public interface OrderService {
  OrderResponse getOrderById(Long id);

  CursorPage<OrderResponse> getOrders(String cursor, int size, OrderFilter filter);

  OrderResponse createOrder(OrderRequest request);

  OrderResponse updateStatus(Long id, UpdateStatusRequest request);

  void confirmOrder(Long orderId);

  void cancelOrder(Long orderId);

  void deleteOrder(Long id);
}
