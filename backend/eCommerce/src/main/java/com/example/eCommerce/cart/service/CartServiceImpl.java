package com.example.eCommerce.cart.service;

import com.example.eCommerce.cart.dtos.AddCartItemRequest;
import com.example.eCommerce.cart.dtos.CartResponse;
import com.example.eCommerce.cart.dtos.UpdateCartItemRequest;
import com.example.eCommerce.cart.entity.Cart;
import com.example.eCommerce.cart.entity.CartItem;
import com.example.eCommerce.cart.repo.CartRepo;
import com.example.eCommerce.common.enums.ErrorCode;
import com.example.eCommerce.exception.BusinessException;
import com.example.eCommerce.product.entity.Product;
import com.example.eCommerce.product.enums.ProductStatus;
import com.example.eCommerce.product.service.ProductService;
import com.example.eCommerce.user.services.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService
{
    private final CartRepo cartRepo;
    private final CartMapper cartMapper;
    private final ProductService productService;
    private final CustomerService customerService;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId)
    {
        // No cart yet is a normal state, not an error - and a GET must not create rows.
        return cartRepo.findByCustomerUserId(userId)
                .map(cartMapper::toResponse)
                .orElseGet(CartResponse::empty);
    }

    @Override
    @Transactional
    public CartResponse addItem(UUID userId, AddCartItemRequest request)
    {
        Product product = productService.getVisibleProduct(request.productId());
        Cart cart = cartRepo.findByCustomerUserId(userId).orElseGet(() -> createCart(userId));

        ensureSameCurrency(cart, product);

        CartItem existing = cart.findItem(product.getProductId()).orElse(null);
        // Validate the RESULTING quantity, not just what is being added: 8 in the cart + 5 more must fail at a limit of 10.
        int newQuantity = (existing == null ? 0 : existing.getQuantity()) + request.quantity();
        ensurePurchasable(product, newQuantity);

        if (existing == null)
            cart.addItem(product, newQuantity);
        else
        {
            existing.setQuantity(newQuantity);
            cart.touch();
        }

        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItem(UUID userId, Integer productId, UpdateCartItemRequest request)
    {
        Cart cart = findCart(userId, productId);
        CartItem item = findItem(cart, productId);

        ensurePurchasable(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());
        cart.touch();

        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(UUID userId, Integer productId)
    {
        Cart cart = findCart(userId, productId);
        // Removing must always work - including lines whose product has since become unavailable.
        cart.removeItem(findItem(cart, productId));
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId)
    {
        cartRepo.findByCustomerUserId(userId).ifPresent(Cart::clear);
    }

    private Cart createCart(UUID userId)
    {
        // Two simultaneous "first adds" would both get here; the UNIQUE constraint on CART.CUSTOMER_ID rejects
        // the second one, which GlobalExceptionHandler turns into a 409 the client can retry.
        Cart cart = cartRepo.save(new Cart(customerService.getCustomerById(userId)));
        log.info("Created cart {} for customer {}", cart.getId(), userId);
        return cart;
    }

    private Cart findCart(UUID userId, Integer productId)
    {
        return cartRepo.findByCustomerUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND, productId));
    }

    private static CartItem findItem(Cart cart, Integer productId)
    {
        return cart.findItem(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND, productId));
    }

    private static void ensurePurchasable(Product product, int quantity)
    {
        if (product.getStatus() != ProductStatus.ACTIVE || product.getStockQuantity() <= 0)
            throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE, product.getProductName());

        // DTO validation already caps a single request; this catches the combined total of repeated adds.
        if (quantity > CartItem.MAX_QUANTITY)
            throw new BusinessException(ErrorCode.CART_QUANTITY_LIMIT, CartItem.MAX_QUANTITY);

        if (quantity > product.getStockQuantity())
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, product.getStockQuantity(), product.getProductName());
    }

    /** One currency per cart: a subtotal of USD + EUR is meaningless. */
    private static void ensureSameCurrency(Cart cart, Product product)
    {
        cart.getCartItems().stream()
                .map(item -> item.getProduct().getCurrency())
                .filter(currency -> !Objects.equals(currency, product.getCurrency()))
                .findFirst()
                .ifPresent(cartCurrency -> {
                    throw new BusinessException(ErrorCode.CART_CURRENCY_MISMATCH,
                            cartCurrency, product.getProductName(), product.getCurrency());
                });
    }
}
