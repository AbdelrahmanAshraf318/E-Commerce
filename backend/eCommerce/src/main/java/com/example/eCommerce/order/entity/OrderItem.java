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
public class OrderItem
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_ITEM_ID")
    private Long id;

    @Column(name = "QUANTITY", nullable = false)
    private Long quantity;

    @Column(name = "ITEM_PRICE", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "PRODUCT_NAME", nullable = false)
    private String productName;

    @Setter(AccessLevel.NONE)
    @Column(name = "TOTAL_PRICE")
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    private Order order;

    @PrePersist
    @PreUpdate
    public void calculateTotalPrice()
    {
        if(Objects.nonNull(quantity) && Objects.nonNull(unitPrice))
            totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}