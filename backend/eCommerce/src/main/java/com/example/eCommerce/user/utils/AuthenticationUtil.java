package com.example.eCommerce.user.utils;

import com.example.eCommerce.user.entity.Customer;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;

import java.util.Objects;
import java.util.UUID;

@UtilityClass
public class AuthenticationUtil
{
    public UUID getUserId(Authentication authentication)
    {
        return ((Customer) Objects.requireNonNull(authentication.getPrincipal())).getUserId();
    }
}
