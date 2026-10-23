package com.example.eCommerce.audit.enums;

/**
 * Stored by name (EnumType.STRING), so constants may be added or reordered freely - but never renamed,
 * or existing rows stop mapping.
 */
public enum AuditAction
{
    // ---- Account ----
    ACCOUNT_REGISTERED,
    SIGNED_IN,
    PROFILE_UPDATED,
    PASSWORD_CHANGED,
    ACCOUNT_DEACTIVATED,
    ACCOUNT_REACTIVATED,
    ACCOUNT_DELETED,

    // ---- Cart ----
    CART_ITEM_ADDED,
    CART_ITEM_UPDATED,
    CART_ITEM_REMOVED,
    CART_CLEARED
}
