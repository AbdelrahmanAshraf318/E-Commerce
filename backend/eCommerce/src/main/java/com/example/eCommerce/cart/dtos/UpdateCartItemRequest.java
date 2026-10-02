package com.example.eCommerce.cart.dtos;

import com.example.eCommerce.cart.entity.CartItem;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Sets the quantity outright. 0 is rejected rather than meaning "remove": removal has its own
 * DELETE endpoint, and one meaning per value keeps the API predictable.
 */
public record UpdateCartItemRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1 - use DELETE to remove the item")
        @Max(value = CartItem.MAX_QUANTITY, message = "Quantity must be at most " + CartItem.MAX_QUANTITY)
        Integer quantity)
{
}
