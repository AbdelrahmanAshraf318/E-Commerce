package com.example.eCommerce.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * "code" is a stable, machine-readable value the frontend can branch on.
 * "defaultMessage" is for humans and may change freely.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode
{
    // ---- Generic ----
    VALIDATION_FAILED("VALIDATION_FAILED", "One or more fields are invalid", HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST("MALFORMED_REQUEST", "Request body is missing or malformed", HttpStatus.BAD_REQUEST),
    DATA_CONFLICT("DATA_CONFLICT", "The request conflicts with existing data", HttpStatus.CONFLICT),
    INTERNAL_ERROR("INTERNAL_ERROR", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR),

    // ---- Authentication / authorization ----
    UNAUTHORIZED("UNAUTHORIZED", "Authentication is required", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("ACCESS_DENIED", "You are not allowed to perform this action", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Invalid email or password", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DEACTIVATED("ACCOUNT_DEACTIVATED", "This account is deactivated", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "This account is locked", HttpStatus.FORBIDDEN),
    OAUTH2_EMAIL_NOT_VERIFIED("OAUTH2_EMAIL_NOT_VERIFIED", "Your %s email address is not verified", HttpStatus.FORBIDDEN),

    // ---- User ----
    USER_NOT_FOUND("USER_NOT_FOUND", "User with id %s was not found", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "An account with this email already exists", HttpStatus.CONFLICT),
    PHONE_ALREADY_EXISTS("PHONE_ALREADY_EXISTS", "This phone number is already used by another account", HttpStatus.CONFLICT),
    CONFIRMED_PASSWORD_ERROR("CONFIRMED_PASSWORD_ERROR", "New password and confirmation do not match", HttpStatus.BAD_REQUEST),
    CURRENT_PASSWORD_REQUIRED("CURRENT_PASSWORD_REQUIRED", "Current password is required", HttpStatus.BAD_REQUEST),
    CURRENT_PASSWORD_INCORRECT("CURRENT_PASSWORD_INCORRECT", "Current password is incorrect", HttpStatus.BAD_REQUEST),
    ACCOUNT_ALREADY_DEACTIVATED("ACCOUNT_ALREADY_DEACTIVATED", "Account is already deactivated", HttpStatus.BAD_REQUEST),
    ACCOUNT_ALREADY_ACTIVATED("ACCOUNT_ALREADY_ACTIVATED", "Account is already active", HttpStatus.BAD_REQUEST),

    // ---- Product ----
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "Product with id %s was not found", HttpStatus.NOT_FOUND),
    PRODUCT_IMAGE_NOT_FOUND("PRODUCT_IMAGE_NOT_FOUND", "Product %s has no image", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_AVAILABLE("PRODUCT_NOT_AVAILABLE", "%s is currently unavailable", HttpStatus.CONFLICT),

    // ---- Cart ----
    CART_ITEM_NOT_FOUND("CART_ITEM_NOT_FOUND", "Product %s is not in your cart", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK", "Only %s of %s left in stock", HttpStatus.CONFLICT),
    CART_QUANTITY_LIMIT("CART_QUANTITY_LIMIT", "You can add at most %s of the same product", HttpStatus.BAD_REQUEST),
    CART_CURRENCY_MISMATCH("CART_CURRENCY_MISMATCH", "Your cart is in %s; %s is priced in %s", HttpStatus.CONFLICT);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;
}
