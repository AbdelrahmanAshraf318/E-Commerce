package com.example.eCommerce.cart.service;

import com.example.eCommerce.cart.dtos.CartResponse;
import com.example.eCommerce.cart.dtos.CartResponse.CartItemResponse;
import com.example.eCommerce.cart.entity.Cart;
import com.example.eCommerce.cart.entity.CartItem;
import com.example.eCommerce.product.entity.Product;
import com.example.eCommerce.product.enums.ProductStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Prices are read from the product NOW, so the cart always shows current prices.
 * Must be called inside the service transaction (items and products are lazy).
 */
@Component
public class CartMapper
{
    public CartResponse toResponse(Cart cart)
    {
        List<CartItemResponse> items = cart.getCartItems().stream().map(this::toItemResponse).toList();

        List<CartItemResponse> available = items.stream().filter(CartItemResponse::available).toList();
        BigDecimal subtotal = available.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalQuantity = available.stream().mapToInt(CartItemResponse::quantity).sum();
        String currency = items.isEmpty() ? null : items.getFirst().currency();

        return new CartResponse(items, totalQuantity, subtotal, currency, available.size() < items.size());
    }

    private CartItemResponse toItemResponse(CartItem item)
    {
        Product product = item.getProduct();
        int stock = product.getStockQuantity();
        boolean available = product.getStatus() == ProductStatus.ACTIVE && stock >= item.getQuantity();

        return new CartItemResponse(
                product.getProductId(),
                product.getProductName(),
                product.getPrice(),
                product.getCurrency(),
                item.getQuantity(),
                product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                stock,
                Math.max(0, Math.min(stock, CartItem.MAX_QUANTITY)),
                available);
    }
}
