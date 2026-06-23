package com.example.eCommerce.order.repository;

import com.example.eCommerce.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepo extends JpaRepository<OrderItem, Long>
{
    @NativeQuery(name = "SELECT * FROM ORDER_ITEMS WHERE ORDER_ID = ?1")
    List<OrderItem> findAllOrderItemByOrderId(Long orderId);
}
