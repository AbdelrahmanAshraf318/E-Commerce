package com.example.eCommerce.order.repository;

import com.example.eCommerce.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepo extends  JpaRepository<Order, Long>
{
    @NativeQuery(value = "SELECT * FROM ORDER WHERE USER_ID = ?1")
    List<Order> findByUserId(UUID userId);

    @NativeQuery(value = "SELECT MAX(TOTAL_PRICE) WHERE USER_ID = ?1")
    Order findMaxOrderByUserId(UUID userId);
}
