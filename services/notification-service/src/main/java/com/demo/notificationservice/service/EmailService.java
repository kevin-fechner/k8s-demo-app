package com.demo.notificationservice.service;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.order.OrderStatusChangedEvent;

public interface EmailService {

    void sendOrderConfirmation(OrderCreatedEvent event);

    void sendStatusUpdate(OrderStatusChangedEvent event);
}
