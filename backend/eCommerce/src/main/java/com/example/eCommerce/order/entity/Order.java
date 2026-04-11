package com.example.eCommerce.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ORDER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> orderItem;

    private LocalDateTime createdAt;
}
