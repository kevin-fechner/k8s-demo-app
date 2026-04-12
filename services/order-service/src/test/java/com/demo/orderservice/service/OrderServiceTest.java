package com.demo.orderservice.service;

import com.demo.orderservice.client.ProductClient;
import com.demo.orderservice.dto.*;
import com.demo.orderservice.entity.*;
import com.demo.orderservice.entity.Order;
import com.demo.orderservice.exception.*;
import com.demo.orderservice.mapper.OrderMapper;
import com.demo.orderservice.repository.OrderRepository;
import com.demo.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private ProductClient productClient;
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
}