package com.example.eCommerce.cart.controller;

import com.example.eCommerce.cart.dtos.AddCartItemRequest;
import com.example.eCommerce.cart.dtos.CartResponse;
import com.example.eCommerce.cart.dtos.UpdateCartItemRequest;
import com.example.eCommerce.cart.service.CartService;
import com.example.eCommerce.user.utils.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Always "my cart": there is no cart id or user id in any path or body. Protected by
 * anyRequest().authenticated() in SecurityConfig - no extra rule needed.
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "The signed-in customer's shopping cart")
@SecurityRequirement(name = "bearerAuth")
public class CartController
{
    private final CartService cartService;

    @GetMapping
    public CartResponse getCart(Authentication authentication)
    {
        return cartService.getCart(AuthenticationUtil.getUserId(authentication));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product (adds to the quantity if it is already in the cart)")
    public CartResponse addItem(@RequestBody @Valid AddCartItemRequest request, Authentication authentication)
    {
        return cartService.addItem(AuthenticationUtil.getUserId(authentication), request);
    }

    @PatchMapping("/items/{productId}")
    @Operation(summary = "Set the quantity of a product already in the cart")
    public CartResponse updateItem(@PathVariable Integer productId,
                                   @RequestBody @Valid UpdateCartItemRequest request,
                                   Authentication authentication)
    {
        return cartService.updateItem(AuthenticationUtil.getUserId(authentication), productId, request);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@PathVariable Integer productId, Authentication authentication)
    {
        return cartService.removeItem(AuthenticationUtil.getUserId(authentication), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(Authentication authentication)
    {
        cartService.clearCart(AuthenticationUtil.getUserId(authentication));
    }
}
