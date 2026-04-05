package com.ecommerce.order.service;

import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.dto.*;
import com.ecommerce.order.exception.*;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    public OrderService(OrderRepository orderRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        logger.info("Creating order for customer: {}", request.getCustomerName());

        // Validate stock and fetch product prices
        List<ProductDto> products = new ArrayList<>();
        for (OrderItemRequest itemRequest : request.getItems()) {
            ProductDto product = productClient.getProductById(itemRequest.getProductId());
            if (product == null) {
                throw new OrderNotFoundException("Product not found: " + itemRequest.getProductId());
            }

            if (product.getQuantity() < itemRequest.getQuantity()) {
                throw new InsufficientStockException(
                        itemRequest.getProductId(),
                        itemRequest.getQuantity(),
                        product.getQuantity()
                );
            }
            products.add(product);
        }

        // Create order
        Order order = new Order(request.getCustomerName());

        // Add items with prices from Product-Service
        for (int i = 0; i < request.getItems().size(); i++) {
            OrderItemRequest itemRequest = request.getItems().get(i);
            ProductDto product = products.get(i);

            OrderItem item = new OrderItem(
                    itemRequest.getProductId(),
                    itemRequest.getQuantity(),
                    product.getPrice()
            );
            order.addItem(item);
        }

        order = orderRepository.save(order);
        logger.info("Order created successfully with id: {}", order.getId());

        return mapToOrderResponse(order, products);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        logger.debug("Fetching all orders");
        return orderRepository.findAll().stream()
                .map(this::mapToOrderResponseWithProducts)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        logger.debug("Fetching order with id: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return mapToOrderResponseWithProducts(order);
    }

    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        logger.info("Updating status of order {} to {}", id, newStatus);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        validateStatusTransition(order.getStatus(), newStatus);

        // If transitioning to VALIDATED, decrease stock for all items
        if (newStatus == OrderStatus.VALIDATED && order.getStatus() == OrderStatus.PENDING) {
            decreaseStockForOrder(order);
        }

        order.setStatus(newStatus);
        order = orderRepository.save(order);

        logger.info("Order {} status updated to {}", id, newStatus);
        return mapToOrderResponseWithProducts(order);
    }

    private void decreaseStockForOrder(Order order) {
        logger.info("Decreasing stock for order: {}", order.getId());
        for (OrderItem item : order.getItems()) {
            try {
                productClient.decreaseStock(item.getProductId(), item.getQuantity());
                logger.debug("Stock decreased for product {} by {}", item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                logger.error("Failed to decrease stock for product {}: {}", item.getProductId(), e.getMessage());
                throw new RuntimeException("Failed to decrease stock for product " + item.getProductId(), e);
            }
        }
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        boolean valid = false;

        switch (currentStatus) {
            case PENDING:
                valid = (newStatus == OrderStatus.VALIDATED);
                break;
            case VALIDATED:
                valid = (newStatus == OrderStatus.SHIPPED);
                break;
            case SHIPPED:
                valid = false; // Terminal state
                break;
        }

        if (!valid) {
            throw new InvalidOrderStatusTransitionException(currentStatus, newStatus);
        }
    }

    private OrderResponse mapToOrderResponse(Order order, List<ProductDto> products) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();

        for (int i = 0; i < order.getItems().size(); i++) {
            OrderItem item = order.getItems().get(i);
            ProductDto product = products.get(i);

            OrderItemResponse itemResponse = new OrderItemResponse(
                    item.getId(),
                    item.getProductId(),
                    product.getName(),
                    item.getQuantity(),
                    item.getPrice(),
                    item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
            );
            itemResponses.add(itemResponse);
        }

        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getDate(),
                itemResponses
        );
    }

    private OrderResponse mapToOrderResponseWithProducts(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> {
                    String productName = "Unknown Product";
                    try {
                        ProductDto product = productClient.getProductById(item.getProductId());
                        if (product != null) {
                            productName = product.getName();
                        }
                    } catch (Exception e) {
                        logger.warn("Could not fetch product name for product {}", item.getProductId());
                    }

                    return new OrderItemResponse(
                            item.getId(),
                            item.getProductId(),
                            productName,
                            item.getQuantity(),
                            item.getPrice(),
                            item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                    );
                })
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getDate(),
                itemResponses
        );
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(String customerName) {
        return orderRepository.findByCustomerName(customerName).stream()
                .map(this::mapToOrderResponseWithProducts)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(this::mapToOrderResponseWithProducts)
                .collect(Collectors.toList());
    }
}
