package com.example.eCommerce.cart.entity;

import com.example.eCommerce.product.entity.Product;
import com.example.eCommerce.user.entity.Customer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One cart per customer. The cart is the aggregate root: items are only ever changed through it,
 * which is why there is no CartItemRepo.
 * <p>
 * Only this side maps the relationship. Mapping it on Customer too would make Hibernate load the cart
 * with every Customer (the inverse side of a @OneToOne cannot be lazy) - i.e. on every authenticated request.
 */
@Entity
@Table(name = "CART")
@Getter
@Setter
@NoArgsConstructor
public class Cart
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CART_ID")
    private Integer id;

    // unique = one cart per customer, enforced by the database.
    // ON DELETE CASCADE: deleting the account deletes the cart, without the user module knowing carts exist.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false, unique = true, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Customer customer;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<CartItem> cartItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    public Cart(Customer customer)
    {
        this.customer = customer;
    }

    public Optional<CartItem> findItem(Integer productId)
    {
        return cartItems.stream()
                .filter(item -> Objects.equals(item.getProduct().getProductId(), productId))
                .findFirst();
    }

    public void addItem(Product product, int quantity)
    {
        cartItems.add(new CartItem(this, product, quantity));
        touch();
    }

    /** orphanRemoval deletes the row once it leaves the list. */
    public void removeItem(CartItem item)
    {
        cartItems.remove(item);
        touch();
    }

    public void clear()
    {
        cartItems.clear();
        touch();
    }

    /** Item changes do not dirty the CART row by themselves; this keeps UPDATED_AT meaningful. */
    public void touch()
    {
        this.updatedAt = LocalDateTime.now();
    }
}
