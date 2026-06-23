package com.example.eCommerce.order.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;


@Entity
@Table(name = "ORDER_ITEMS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_ITEM_ID")
    private Long id;

    @Column(name = "QUANTITY")
    private Long quantity;

    @Column(name = "ITEM_PRICE")
    private BigDecimal unitPrice;

    @Column(name = "PRODUCT_NAME")
    private String productName;

    @Setter(AccessLevel.NONE) // Cannot access set for this attribute
    @Column(name = "TOTAL_PRICE")
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId")
    private Order order;

    @PrePersist
    @PreUpdate
    public void calculateTotalPrice()
    {
        if(Objects.nonNull(quantity) && Objects.nonNull(unitPrice))
            totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
