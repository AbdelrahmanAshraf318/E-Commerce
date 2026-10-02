package com.example.eCommerce.user.services;

import com.example.eCommerce.user.dtos.AuthResponse;
import com.example.eCommerce.user.dtos.CreateUserRequest;
import com.example.eCommerce.user.dtos.UserLoginRequest;
import com.example.eCommerce.user.enums.AuthProvider;

public interface AuthService
{
    AuthResponse register(CreateUserRequest createUserRequest);

    AuthResponse login(UserLoginRequest userLoginRequest);

    /**
     * A deactivated account cannot log in, so it cannot call an authenticated "reactivate" endpoint either.
     * Reactivation therefore re-checks the credentials itself.
     */
    AuthResponse reactivate(UserLoginRequest userLoginRequest);

    /**
     * Finds or creates the customer for a provider-verified identity and returns an access token.
     */
    String loginWithOAuth2(AuthProvider provider, String email, String name, boolean emailVerified);
}
