package com.example.eCommerce.user.enums;

public enum AccountStatus
{
    PENDING_APPROVAL, // signed up, waiting for an admin
    ACTIVE,           // approved, can sign in
    REJECTED,         // an admin said no
    DEACTIVATED       // was active, switched off by an admin
}
