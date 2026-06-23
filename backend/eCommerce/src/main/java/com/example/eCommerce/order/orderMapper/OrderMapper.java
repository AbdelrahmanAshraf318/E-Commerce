package com.example.eCommerce.order.orderMapper;

import com.example.eCommerce.order.dtos.OrderDto;
import com.example.eCommerce.order.entity.Order;
import com.example.eCommerce.user.entity.Customer;
import org.springframework.util.CollectionUtils;

import java.util.Objects;

public final class OrderMapper
{

    public static Order mapToEntity(OrderDto orderDto, Customer customer) throws RuntimeException
    {
        if (Objects.isNull(orderDto))
            throw new IllegalArgumentException("OrderDto must not be null");
        if (Objects.isNull(customer))
            throw new IllegalArgumentException("Customer must not be null");
        if (CollectionUtils.isEmpty(orderDto.getOrderItems()))
            throw new IllegalArgumentException("Order must contain at least one item");

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderItem(orderDto.getOrderItems());
        return order;
    }
}
