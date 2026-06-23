package com.example.eCommerce.order.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto
{
    private Long quantity;
    private Double unitPrice;
    private String productName;
}
