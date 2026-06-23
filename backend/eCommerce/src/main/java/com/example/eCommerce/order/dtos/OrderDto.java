package com.example.eCommerce.order.dtos;

import com.example.eCommerce.order.entity.OrderItem;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Builder
public class OrderDto
{
    private List<OrderItem> orderItems;
    private UUID userId;
}

