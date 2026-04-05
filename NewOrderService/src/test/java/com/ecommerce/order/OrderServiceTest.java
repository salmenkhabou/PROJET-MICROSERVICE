package com.ecommerce.order;

import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.dto.*;
import com.ecommerce.order.exception.*;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderService orderService;

    private ProductDto testProduct;
    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        testProduct = new ProductDto(1L, "Test Product", new BigDecimal("99.99"), 100, "Electronics");

        OrderItemRequest itemRequest = new OrderItemRequest(1L, 2);
        createOrderRequest = new CreateOrderRequest("John Doe", Arrays.asList(itemRequest));
    }

    @Test
    void createOrder_WithValidData_ReturnsOrderResponse() {
        when(productClient.getProductById(1L)).thenReturn(testProduct);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        OrderResponse response = orderService.createOrder(createOrderRequest);

        assertNotNull(response);
        assertEquals("John Doe", response.getCustomerName());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(1, response.getItems().size());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createOrder_WithInsufficientStock_ThrowsException() {
        testProduct.setQuantity(1); // Only 1 available, but 2 requested
        when(productClient.getProductById(1L)).thenReturn(testProduct);

        assertThrows(InsufficientStockException.class, () -> orderService.createOrder(createOrderRequest));
    }

    @Test
    void getOrderById_WhenExists_ReturnsOrder() {
        Order order = new Order("John Doe");
        order.setId(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productClient.getProductById(anyLong())).thenReturn(testProduct);

        OrderResponse response = orderService.getOrderById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getOrderById_WhenNotExists_ThrowsException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(99L));
    }

    @Test
    void updateOrderStatus_ValidTransition_UpdatesStatus() {
        Order order = new Order("John Doe");
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(productClient.decreaseStock(anyLong(), anyInt())).thenReturn(testProduct);
        when(productClient.getProductById(anyLong())).thenReturn(testProduct);

        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.VALIDATED);

        assertEquals(OrderStatus.VALIDATED, response.getStatus());
    }

    @Test
    void updateOrderStatus_InvalidTransition_ThrowsException() {
        Order order = new Order("John Doe");
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStatusTransitionException.class, 
                () -> orderService.updateOrderStatus(1L, OrderStatus.SHIPPED));
    }

    @Test
    void updateOrderStatus_FromShipped_ThrowsException() {
        Order order = new Order("John Doe");
        order.setId(1L);
        order.setStatus(OrderStatus.SHIPPED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStatusTransitionException.class, 
                () -> orderService.updateOrderStatus(1L, OrderStatus.VALIDATED));
    }

    @Test
    void getAllOrders_ReturnsListOfOrders() {
        Order order1 = new Order("John Doe");
        order1.setId(1L);
        Order order2 = new Order("Jane Doe");
        order2.setId(2L);

        when(orderRepository.findAll()).thenReturn(Arrays.asList(order1, order2));
        when(productClient.getProductById(anyLong())).thenReturn(testProduct);

        List<OrderResponse> orders = orderService.getAllOrders();

        assertEquals(2, orders.size());
    }
}
