package com.example.eCommerce.cart.dtos;

import com.example.eCommerce.cart.entity.CartItem;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Adds to the quantity already in the cart (adding 2 when 1 is there gives 3). */
public record AddCartItemRequest(
        @NotNull(message = "Product is required")
        Integer productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = CartItem.MAX_QUANTITY, message = "Quantity must be at most " + CartItem.MAX_QUANTITY)
        Integer quantity)
{
}
