package com.ecommerce.order.repository;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerName(String customerName);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByDateBetween(LocalDateTime start, LocalDateTime end);

    List<Order> findByCustomerNameAndStatus(String customerName, OrderStatus status);
}
