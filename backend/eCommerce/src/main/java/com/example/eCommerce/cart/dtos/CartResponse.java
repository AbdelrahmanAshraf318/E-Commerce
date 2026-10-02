package com.example.eCommerce.cart.dtos;

import java.math.BigDecimal;
import java.util.List;

/**
 * Returned by every cart endpoint, so the client never has to compute totals itself.
 *
 * @param subtotal          sum of AVAILABLE lines only - what the customer could actually buy right now
 * @param totalQuantity     units across available lines (for the navbar badge)
 * @param currency          null while the cart is empty
 * @param hasUnavailableItems true if any line can't be bought as-is; checkout must refuse until fixed
 */
public record CartResponse(List<CartItemResponse> items,
                           int totalQuantity,
                           BigDecimal subtotal,
                           String currency,
                           boolean hasUnavailableItems)
{
    public static CartResponse empty()
    {
        return new CartResponse(List.of(), 0, BigDecimal.ZERO, null, false);
    }

    /**
     * @param available    false if the product went IN_ACTIVE / out of stock, or stock dropped below the
     *                     quantity, after it was added. The line is kept so the customer sees what changed.
     * @param maxQuantity  what the quantity selector may go up to: min(stock, per-line limit)
     */
    public record CartItemResponse(Integer productId,
                                   String productName,
                                   BigDecimal unitPrice,
                                   String currency,
                                   int quantity,
                                   BigDecimal lineTotal,
                                   int stockQuantity,
                                   int maxQuantity,
                                   boolean available)
    {
    }
}
