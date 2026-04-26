package com.demo.orderservice.service;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.order.OrderStatusChangedEvent;
import com.demo.orderservice.client.ProductClient;
import com.demo.orderservice.dto.*;
import com.demo.orderservice.entity.*;
import com.demo.orderservice.entity.Order;
import com.demo.orderservice.exception.*;
import com.demo.orderservice.mapper.OrderMapper;
import com.demo.orderservice.messaging.OrderEventPublisher;
import com.demo.orderservice.repository.OrderRepository;
import com.demo.orderservice.service.impl.OrderServiceImpl;
import com.demo.orderservice.util.CursorUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private ProductClient productClient;
    @Mock private OrderEventPublisher eventPublisher;
    @Spy private MeterRegistry meterRegistry = new SimpleMeterRegistry();
    @InjectMocks private OrderServiceImpl orderService;

    private Order testOrder;
    private OrderResponse testResponse;
    private OrderRequest testRequest;
    private ProductDto testProduct;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(1L)
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("99.99"))
                .items(new ArrayList<>())
                .build();

        testResponse = new OrderResponse(
                1L, "John Doe", "john@example.com",
                OrderStatus.PENDING, new BigDecimal("99.99"),
                null, List.of(), null, null);

        testRequest = new OrderRequest(
                "John Doe", "john@example.com",
                List.of(new OrderItemRequest(1L, 2)),
                null);

        testProduct = new ProductDto(
                1L, "Test Product", "Description",
                new BigDecimal("49.99"), 100);
    }

    @Test
    @DisplayName("Should return all orders")
    void getAllOrders_ReturnsAllOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(testOrder));
        when(orderMapper.toResponse(testOrder)).thenReturn(testResponse);

        List<OrderResponse> result = orderService.getAllOrders();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().customerName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should return order by id")
    void getOrderById_ExistingId_ReturnsOrder() {
        when(orderRepository.findByIdWithItems(1L))
                .thenReturn(Optional.of(testOrder));
        when(orderMapper.toResponse(testOrder)).thenReturn(testResponse);

        OrderResponse result = orderService.getOrderById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.customerName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void getOrderById_NonExistingId_ThrowsException() {
        when(orderRepository.findByIdWithItems(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Should create order successfully")
    void createOrder_ValidRequest_ReturnsCreatedOrder() {
        when(orderMapper.toOrder(testRequest)).thenReturn(testOrder);
        when(productClient.getProductById(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toResponse(testOrder)).thenReturn(testResponse);

        OrderResponse result = orderService.createOrder(testRequest);

        assertThat(result.customerName()).isEqualTo("John Doe");
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(eventPublisher).publishOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when product not available")
    void createOrder_ProductNotAvailable_ThrowsException() {
        when(orderMapper.toOrder(testRequest)).thenReturn(testOrder);
        when(productClient.getProductById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(testRequest))
                .isInstanceOf(ProductNotAvailableException.class);
    }

    @Test
    @DisplayName("Should update order status")
    void updateStatus_ExistingId_ReturnsUpdatedOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toResponse(testOrder)).thenReturn(testResponse);

        OrderResponse result = orderService.updateStatus(
                1L, new UpdateStatusRequest(OrderStatus.CONFIRMED));

        assertThat(result).isNotNull();
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(eventPublisher).publishOrderStatusChanged(any(OrderStatusChangedEvent.class));
    }

    @Test
    @DisplayName("Should confirm order and publish status changed event")
    void confirmOrder_ExistingOrder_SetsConfirmedStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        orderService.confirmOrder(1L);

        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(eventPublisher).publishOrderStatusChanged(any(OrderStatusChangedEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when confirming non-existing order")
    void confirmOrder_NonExistingOrder_ThrowsException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.confirmOrder(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Should cancel order and publish status changed event")
    void cancelOrder_ExistingOrder_SetsCancelledStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        orderService.cancelOrder(1L);

        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(eventPublisher).publishOrderStatusChanged(any(OrderStatusChangedEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when cancelling non-existing order")
    void cancelOrder_NonExistingOrder_ThrowsException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Should delete order successfully")
    void deleteOrder_ExistingId_DeletesOrder() {
        when(orderRepository.existsById(1L)).thenReturn(true);

        orderService.deleteOrder(1L);

        verify(orderRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existing order")
    void deleteOrder_NonExistingId_ThrowsException() {
        when(orderRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> orderService.deleteOrder(99L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("Should return empty cursor page when no orders match")
    void getOrders_NoResults_ReturnsEmptyPage() {
        when(orderRepository.findWithCursor(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        CursorPage<OrderResponse> result = orderService.getOrders(
                null, 10, new OrderFilter(null, null, null, null));

        assertThat(result.data()).isEmpty();
        assertThat(result.hasMore()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    @DisplayName("Should return page with hasMore=false when results fit within page size")
    void getOrders_ResultsWithinPageSize_HasMoreFalse() {
        when(orderRepository.findWithCursor(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(testOrder));
        when(orderMapper.toResponse(testOrder)).thenReturn(testResponse);

        CursorPage<OrderResponse> result = orderService.getOrders(
                null, 10, new OrderFilter(null, null, null, null));

        assertThat(result.data()).hasSize(1);
        assertThat(result.hasMore()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    @DisplayName("Should set hasMore=true and encode the last ID as nextCursor when results exceed page size")
    void getOrders_ResultsExceedPageSize_HasMoreTrueWithNextCursor() {
        List<Order> orders = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            orders.add(Order.builder()
                    .id((long) i)
                    .customerName("C" + i)
                    .customerEmail("c" + i + "@test.com")
                    .status(OrderStatus.PENDING)
                    .totalAmount(BigDecimal.TEN)
                    .items(new ArrayList<>())
                    .build());
        }
        when(orderRepository.findWithCursor(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(orders);
        orders.subList(0, 10).forEach(o -> when(orderMapper.toResponse(o)).thenReturn(testResponse));

        CursorPage<OrderResponse> result = orderService.getOrders(
                null, 10, new OrderFilter(null, null, null, null));

        assertThat(result.data()).hasSize(10);
        assertThat(result.hasMore()).isTrue();
        assertThat(CursorUtil.decode(result.nextCursor())).isEqualTo(10L);
    }

    @Test
    @DisplayName("Should decode cursor and pass the correct ID to the repository")
    void getOrders_WithCursor_PassesCursorIdToRepository() {
        String cursor = CursorUtil.encode(5L);
        when(orderRepository.findWithCursor(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(testOrder));
        when(orderMapper.toResponse(testOrder)).thenReturn(testResponse);

        orderService.getOrders(cursor, 10, new OrderFilter(null, null, null, null));

        verify(orderRepository).findWithCursor(eq(5L), any(), any(), any(), any(), any(Pageable.class));
    }
}