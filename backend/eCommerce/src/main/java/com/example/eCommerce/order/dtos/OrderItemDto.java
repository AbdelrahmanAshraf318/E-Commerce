package com.example.eCommerce.order.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto
{
    private Long quantity;
    // BigDecimal, not Double: money must not pick up binary rounding errors (0.1 + 0.2 != 0.3 as a double).
    private BigDecimal unitPrice;
    private String productName;
    private BigDecimal totalPrice;
}
