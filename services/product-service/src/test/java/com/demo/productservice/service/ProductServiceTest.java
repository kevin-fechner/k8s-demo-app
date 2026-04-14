package com.demo.productservice.service;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.inventory.StockInsufficientEvent;
import com.demo.events.inventory.StockUpdatedEvent;
import com.demo.productservice.dto.*;
import com.demo.productservice.entity.Product;
import com.demo.productservice.exception.ProductNotFoundException;
import com.demo.productservice.mapper.ProductMapper;
import com.demo.productservice.messaging.InventoryEventPublisher;
import com.demo.productservice.repository.ProductRepository;
import com.demo.productservice.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ProductMapper productMapper;

	@Mock
	private InventoryEventPublisher inventoryEventPublisher;

	@InjectMocks
	private ProductServiceImpl productService;

	private Product testProduct;
	private ProductRequest testRequest;
	private ProductResponse testResponse;

	@BeforeEach
	void setUp() {
		testProduct = Product.builder()
				.id(1L)
				.name("Test Product")
				.description("Test Description")
				.price(new BigDecimal("99.99"))
				.stock(10)
				.build();

		testRequest = new ProductRequest("Test Product", "Test Description", new BigDecimal("99.99"), 10);
		testResponse = new ProductResponse(1L, "Test Product", "Test Description", new BigDecimal("99.99"), 10, null, null);
	}

	@Test
	@DisplayName("Should return all products")
	void getAllProducts_ReturnsAllProducts() {
		when(productRepository.findAll()).thenReturn(List.of(testProduct));
		when(productMapper.toResponse(testProduct)).thenReturn(testResponse);

		List<ProductResponse> result = productService.getAllProducts();

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().name()).isEqualTo("Test Product");
		verify(productRepository, times(1)).findAll();
	}

	@Test
	@DisplayName("Should return product by id")
	void getProductById_ExistingId_ReturnsProduct() {
		when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
		when(productMapper.toResponse(testProduct)).thenReturn(testResponse);

		ProductResponse result = productService.getProductById(1L);

		assertThat(result.id()).isEqualTo(1L);
		assertThat(result.name()).isEqualTo("Test Product");
	}

	@Test
	@DisplayName("Should throw exception when product not found")
	void getProductById_NonExistingId_ThrowsException() {
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.getProductById(99L))
				.isInstanceOf(ProductNotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	@DisplayName("Should create product successfully")
	void createProduct_ValidRequest_ReturnsCreatedProduct() {
		when(productMapper.toProduct(testRequest)).thenReturn(testProduct);
		when(productMapper.toResponse(testProduct)).thenReturn(testResponse);
		when(productRepository.save(any(Product.class))).thenReturn(testProduct);

		ProductResponse result = productService.createProduct(testRequest);

		assertThat(result.name()).isEqualTo("Test Product");
		assertThat(result.price()).isEqualByComparingTo("99.99");
		verify(productRepository, times(1)).save(any(Product.class));
	}

	@Test
	@DisplayName("Should update product successfully")
	void updateProduct_ExistingId_ReturnsUpdatedProduct() {
		when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
		when(productMapper.toResponse(testProduct)).thenReturn(testResponse);
		when(productRepository.save(any(Product.class))).thenReturn(testProduct);

		ProductResponse result = productService.updateProduct(1L, testRequest);

		assertThat(result.name()).isEqualTo("Test Product");
		verify(productRepository, times(1)).save(any(Product.class));
	}

	@Test
	@DisplayName("Should delete product successfully")
	void deleteProduct_ExistingId_DeletesProduct() {
		when(productRepository.existsById(1L)).thenReturn(true);

		productService.deleteProduct(1L);

		verify(productRepository, times(1)).deleteById(1L);
	}

	@Test
	@DisplayName("Should throw exception when deleting non-existing product")
	void deleteProduct_NonExistingId_ThrowsException() {
		when(productRepository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> productService.deleteProduct(99L))
				.isInstanceOf(ProductNotFoundException.class);
	}

	@Test
	@DisplayName("Should throw exception when updating non-existing product")
	void updateProduct_NonExistingId_ThrowsException() {
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.updateProduct(99L, testRequest))
				.isInstanceOf(ProductNotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	@DisplayName("Should deduct stock and publish StockUpdatedEvent when stock is sufficient")
	void reserveStock_SufficientStock_DeductsStockAndPublishesStockUpdated() {
		OrderCreatedEvent event = new OrderCreatedEvent(
				1L, "John Doe", "john@example.com",
				List.of(new OrderCreatedEvent.OrderItem(1L, "Widget", 3, new BigDecimal("9.99"))),
				new BigDecimal("29.97"),
				LocalDateTime.of(2024, 1, 15, 10, 0, 0)
		);
		when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
		when(productRepository.save(any(Product.class))).thenReturn(testProduct);

		productService.reserveStock(event);

		assertThat(testProduct.getStock()).isEqualTo(7);
		verify(productRepository).save(testProduct);
		verify(inventoryEventPublisher).publishStockUpdated(any(StockUpdatedEvent.class));
	}

	@Test
	@DisplayName("Should publish StockInsufficientEvent when requested quantity exceeds stock")
	void reserveStock_InsufficientStock_PublishesStockInsufficient() {
		OrderCreatedEvent event = new OrderCreatedEvent(
				1L, "John Doe", "john@example.com",
				List.of(new OrderCreatedEvent.OrderItem(1L, "Widget", 20, new BigDecimal("9.99"))),
				new BigDecimal("199.80"),
				LocalDateTime.of(2024, 1, 15, 10, 0, 0)
		);
		when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

		productService.reserveStock(event);

		verify(productRepository, never()).save(any());
		verify(inventoryEventPublisher).publishStockInsufficient(any(StockInsufficientEvent.class));
	}

	@Test
	@DisplayName("Should skip item silently when product is not found during stock reservation")
	void reserveStock_ProductNotFound_SkipsItem() {
		OrderCreatedEvent event = new OrderCreatedEvent(
				1L, "John Doe", "john@example.com",
				List.of(new OrderCreatedEvent.OrderItem(99L, "Ghost", 1, new BigDecimal("9.99"))),
				new BigDecimal("9.99"),
				LocalDateTime.of(2024, 1, 15, 10, 0, 0)
		);
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatNoException().isThrownBy(() -> productService.reserveStock(event));
		verify(productRepository, never()).save(any());
		verify(inventoryEventPublisher, never()).publishStockUpdated(any());
		verify(inventoryEventPublisher, never()).publishStockInsufficient(any());
	}
}