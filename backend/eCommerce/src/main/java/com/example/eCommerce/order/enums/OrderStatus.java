package com.example.eCommerce.order.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum OrderStatus
{
    SAVED_TO_CART(0),
    PENDING_PAYMENT(1),
    CONFIRMED(2),
    IN_PROGRESS(3),
    ON_HOLD(4),
    CANCELED(5),
    COMPLETED(6),
    REQUEST_REFUND(7);

    private final int code;

    public static boolean contains(int code)
    {
        return Arrays.stream(OrderStatus.values()).
                anyMatch(order -> order.getCode() == code);
    }

    public static Optional<OrderStatus> fromCode(int code)
    {
        return Arrays.stream(OrderStatus.values())
                .filter(order -> order.getCode() == code)
                .findFirst();
    }
}
