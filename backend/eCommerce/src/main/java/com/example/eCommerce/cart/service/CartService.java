package com.example.eCommerce.cart.service;

import com.example.eCommerce.cart.dtos.AddCartItemRequest;
import com.example.eCommerce.cart.dtos.CartResponse;
import com.example.eCommerce.cart.dtos.UpdateCartItemRequest;

import java.util.UUID;

/**
 * The signed-in customer's cart. The userId always comes from the Authentication, never from the request,
 * so a customer can only ever read or change their own cart.
 * <p>
 * Stock is CHECKED here but never RESERVED - reserving/decrementing stock is checkout's job.
 */
public interface CartService
{
    /** Side-effect free: returns an empty cart if the customer has never added anything. */
    CartResponse getCart(UUID userId);

    /** Creates the cart on first use. Adds to the existing quantity if the product is already in the cart. */
    CartResponse addItem(UUID userId, AddCartItemRequest request);

    CartResponse updateItem(UUID userId, Integer productId, UpdateCartItemRequest request);

    CartResponse removeItem(UUID userId, Integer productId);

    void clearCart(UUID userId);
}
