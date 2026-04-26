package com.demo.orderservice.controller;

import com.demo.orderservice.dto.*;
import com.demo.orderservice.entity.OrderStatus;
import com.demo.orderservice.exception.OrderNotFoundException;
import com.demo.orderservice.exception.ProductNotAvailableException;
import com.demo.orderservice.service.OrderService;
import com.demo.orderservice.util.CursorUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.test.autoconfigure.webmvc.SecurityMockMvcAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = OrderController.class,
        excludeAutoConfiguration = {
                ServletWebSecurityAutoConfiguration.class,
                SecurityMockMvcAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        }
)
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private OrderService orderService;

    private OrderResponse testResponse;
    private OrderRequest testRequest;

    @BeforeEach
    void setUp() {
        testResponse = new OrderResponse(
                1L, "John Doe", "john@example.com",
                OrderStatus.PENDING, new BigDecimal("99.99"),
                null, List.of(), null, null);

        testRequest = new OrderRequest(
                "John Doe", "john@example.com",
                List.of(new OrderItemRequest(1L, 2)),
                null);
    }

    @Test
    @DisplayName("GET /api/orders - should return paginated response")
    void getOrders_NoParams_Returns200() throws Exception {
        CursorPage<OrderResponse> page = new CursorPage<>(List.of(testResponse), null, false, 1);
        when(orderService.getOrders(isNull(), eq(10), any(OrderFilter.class))).thenReturn(page);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].customerName").value("John Doe"))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    @DisplayName("GET /api/orders?cursor=... - should pass cursor to service")
    void getOrders_WithCursor_Returns200() throws Exception {
        String cursor = CursorUtil.encode(5L);
        CursorPage<OrderResponse> page = new CursorPage<>(List.of(testResponse), null, false, 1);
        when(orderService.getOrders(eq(cursor), eq(10), any(OrderFilter.class))).thenReturn(page);

        mockMvc.perform(get("/api/orders").param("cursor", cursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].customerName").value("John Doe"));
    }

    @Test
    @DisplayName("GET /api/orders?size=200 - should cap page size at 100")
    void getOrders_SizeExceeds100_CappedAt100() throws Exception {
        CursorPage<OrderResponse> page = new CursorPage<>(List.of(), null, false, 0);
        when(orderService.getOrders(isNull(), eq(100), any(OrderFilter.class))).thenReturn(page);

        mockMvc.perform(get("/api/orders").param("size", "200"))
                .andExpect(status().isOk());

        verify(orderService).getOrders(isNull(), eq(100), any(OrderFilter.class));
    }

    @Test
    @DisplayName("GET /api/orders?status=PENDING - should pass status filter to service")
    void getOrders_WithStatusFilter_PassesFilterToService() throws Exception {
        CursorPage<OrderResponse> page = new CursorPage<>(List.of(testResponse), null, false, 1);
        when(orderService.getOrders(any(), anyInt(), any(OrderFilter.class))).thenReturn(page);

        mockMvc.perform(get("/api/orders").param("status", "PENDING"))
                .andExpect(status().isOk());

        verify(orderService).getOrders(
                isNull(), eq(10), eq(new OrderFilter(OrderStatus.PENDING, null, null, null)));
    }

    @Test
    @DisplayName("GET /api/orders - should return nextCursor in response when hasMore=true")
    void getOrders_HasMore_ReturnsNextCursor() throws Exception {
        String nextCursor = CursorUtil.encode(10L);
        CursorPage<OrderResponse> page = new CursorPage<>(List.of(testResponse), nextCursor, true, 10);
        when(orderService.getOrders(any(), anyInt(), any())).thenReturn(page);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextCursor").value(nextCursor));
    }

    @Test
    @DisplayName("GET /api/orders/{id} - should return order")
    void getOrderById_Returns200() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(testResponse);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/orders/{id} - should return 404 when not found")
    void getOrderById_Returns404() throws Exception {
        when(orderService.getOrderById(99L))
                .thenThrow(new OrderNotFoundException(99L));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/orders - should create order")
    void createOrder_Returns201() throws Exception {
        when(orderService.createOrder(any())).thenReturn(testResponse);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("John Doe"));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status - should update status")
    void updateStatus_Returns200() throws Exception {
        when(orderService.updateStatus(any(), any())).thenReturn(testResponse);

        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateStatusRequest(OrderStatus.CONFIRMED))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/orders/{id} - should delete order")
    void deleteOrder_Returns204() throws Exception {
        doNothing().when(orderService).deleteOrder(1L);

        mockMvc.perform(delete("/api/orders/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/orders - should return 422 when product not available")
    void createOrder_ProductNotAvailable_Returns422() throws Exception {
        when(orderService.createOrder(any()))
                .thenThrow(new ProductNotAvailableException(1L));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().is(422));
    }

    @Test
    @DisplayName("POST /api/orders - should return 400 when request is invalid")
    void createOrder_InvalidRequest_Returns400() throws Exception {
        OrderRequest invalid = new OrderRequest(null, "john@example.com",
                List.of(new OrderItemRequest(1L, 2)), null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/orders/{id} - should return 404 when not found")
    void deleteOrder_Returns404() throws Exception {
        doThrow(new OrderNotFoundException(99L)).when(orderService).deleteOrder(99L);

        mockMvc.perform(delete("/api/orders/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status - should return 404 when not found")
    void updateStatus_Returns404() throws Exception {
        when(orderService.updateStatus(eq(99L), any()))
                .thenThrow(new OrderNotFoundException(99L));

        mockMvc.perform(patch("/api/orders/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateStatusRequest(OrderStatus.CONFIRMED))))
                .andExpect(status().isNotFound());
    }
}
