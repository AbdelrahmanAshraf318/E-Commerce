package com.example.eCommerce.user.utils;

import com.example.eCommerce.user.entity.Customer;
import org.springframework.security.core.Authentication;

import java.util.Objects;
import java.util.UUID;

public class AuthenticationUtil
{
    public static UUID getUserId(Authentication authentication)
    {
        return ((Customer) Objects.requireNonNull(authentication.getPrincipal())).getUserId();
    }
}
