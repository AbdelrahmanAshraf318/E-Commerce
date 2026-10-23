package com.example.eCommerce.user.dtos;


import com.example.eCommerce.order.dtos.OrderItemDto;
import com.example.eCommerce.order.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOrderDto
{
    private Long orderId;
    private List<OrderItemDto> orderItems;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;
    private BigDecimal totalPrice;
    private OrderStatus orderStatus;
}
