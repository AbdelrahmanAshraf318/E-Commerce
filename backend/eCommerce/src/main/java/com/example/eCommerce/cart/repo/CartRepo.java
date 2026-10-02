package com.example.eCommerce.cart.repo;

import com.example.eCommerce.cart.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepo extends JpaRepository<Cart, Integer>
{
    // Cart + items + their products in ONE query (open-in-view is off, and this avoids N+1 per line).
    @EntityGraph(attributePaths = {"cartItems", "cartItems.product"})
    Optional<Cart> findByCustomerUserId(UUID userId);
}
