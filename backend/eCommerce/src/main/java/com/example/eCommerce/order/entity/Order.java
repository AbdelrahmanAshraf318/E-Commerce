package com.example.eCommerce.order.entity;

import com.example.eCommerce.order.enums.OrderStatus;
import com.example.eCommerce.user.entity.Customer;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "ORDER")
public class Order
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_ID")
    private Long orderId;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> orderItem;

    @ManyToOne
    private Customer customer;

    @CreatedDate
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "UPDATED_AT", insertable = false)
    private LocalDateTime updatedAt;

    @Setter(AccessLevel.NONE) // Cannot access set for this attribute
    @Column(name = "TOTAL_PRICE")
    private BigDecimal totalOrderPrice;

    @Column(name = "ORDER_STATUS")
    private OrderStatus orderStatus;

    @PrePersist
    @PreUpdate
    public void calculateTotalPrice()
    {
        if(!CollectionUtils.isEmpty(orderItem))
        {
            totalOrderPrice = orderItem.stream().map(OrderItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
