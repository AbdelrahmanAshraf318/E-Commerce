package com.example.eCommerce.cart.entity;

import com.example.eCommerce.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * One line per product: adding the same product again increases the quantity.
 * There is deliberately no price column - the cart always shows the product's CURRENT price.
 * Prices are copied (snapshotted) into OrderItem only at checkout.
 */
@Entity
@Table(
        name = "CART_ITEM",
        uniqueConstraints = @UniqueConstraint(name = "UK_CART_ITEM_CART_PRODUCT", columnNames = {"CART_ID", "PRODUCT_ID"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA only; use Cart.addItem
public class CartItem
{
    public static final int MAX_QUANTITY = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ITEM_ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CART_ID", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Cart cart;

    // If a product is ever hard-deleted it simply disappears from carts.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PRODUCT_ID", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Product product;

    @Column(name = "QUANTITY", nullable = false)
    private int quantity;

    // Two tabs incrementing the same line at once: the second write fails (409) instead of silently losing an update.
    @Version
    @Column(name = "VERSION", nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    CartItem(Cart cart, Product product, int quantity)
    {
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
    }
}
