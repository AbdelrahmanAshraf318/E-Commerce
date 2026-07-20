package com.example.eCommerce.order.repository;

import com.example.eCommerce.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepo extends JpaRepository<OrderItem, Long>
{
    @Query("SELECT oi FROM OrderItem oi WHERE oi.orders.orderId = :orderId")
    List<OrderItem> findAllByOrderId(@Param("orderId") Long orderId);
}
