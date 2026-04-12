package com.demo.orderservice.mapper;

import com.demo.orderservice.dto.OrderItemRequest;
import com.demo.orderservice.dto.OrderItemResponse;
import com.demo.orderservice.dto.OrderRequest;
import com.demo.orderservice.dto.OrderResponse;
import com.demo.orderservice.entity.Order;
import com.demo.orderservice.entity.OrderItem;
import com.demo.orderservice.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-12T21:54:27+0200",
    comments = "version: 1.6.0, compiler: Eclipse JDT (IDE) 3.45.0.v20260224-0835, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class OrderMapperImpl implements OrderMapper {

    @Override
    public OrderItemResponse toItemResponse(OrderItem item) {
        if ( item == null ) {
            return null;
        }

        Long id = null;
        Long productId = null;
        String productName = null;
        Integer quantity = null;
        BigDecimal unitPrice = null;

        id = item.getId();
        productId = item.getProductId();
        productName = item.getProductName();
        quantity = item.getQuantity();
        unitPrice = item.getUnitPrice();

        BigDecimal subtotal = item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity()));

        OrderItemResponse orderItemResponse = new OrderItemResponse( id, productId, productName, quantity, unitPrice, subtotal );

        return orderItemResponse;
    }

    @Override
    public OrderResponse toResponse(Order order) {
        if ( order == null ) {
            return null;
        }

        List<OrderItemResponse> items = null;
        Long id = null;
        String customerName = null;
        String customerEmail = null;
        OrderStatus status = null;
        BigDecimal totalAmount = null;
        String notes = null;
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;

        items = orderItemListToOrderItemResponseList( order.getItems() );
        id = order.getId();
        customerName = order.getCustomerName();
        customerEmail = order.getCustomerEmail();
        status = order.getStatus();
        totalAmount = order.getTotalAmount();
        notes = order.getNotes();
        createdAt = order.getCreatedAt();
        updatedAt = order.getUpdatedAt();

        OrderResponse orderResponse = new OrderResponse( id, customerName, customerEmail, status, totalAmount, notes, items, createdAt, updatedAt );

        return orderResponse;
    }

    @Override
    public OrderItem toOrderItem(OrderItemRequest request) {
        if ( request == null ) {
            return null;
        }

        OrderItem.OrderItemBuilder orderItem = OrderItem.builder();

        orderItem.productId( request.productId() );
        orderItem.quantity( request.quantity() );

        return orderItem.build();
    }

    @Override
    public Order toOrder(OrderRequest request) {
        if ( request == null ) {
            return null;
        }

        Order.OrderBuilder order = Order.builder();

        order.customerEmail( request.customerEmail() );
        order.customerName( request.customerName() );
        order.notes( request.notes() );

        return order.build();
    }

    protected List<OrderItemResponse> orderItemListToOrderItemResponseList(List<OrderItem> list) {
        if ( list == null ) {
            return null;
        }

        List<OrderItemResponse> list1 = new ArrayList<OrderItemResponse>( list.size() );
        for ( OrderItem orderItem : list ) {
            list1.add( toItemResponse( orderItem ) );
        }

        return list1;
    }
}
