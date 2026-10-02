package com.example.eCommerce.user.controller;

import com.example.eCommerce.user.dtos.AuthResponse;
import com.example.eCommerce.user.dtos.CreateUserRequest;
import com.example.eCommerce.user.dtos.UserLoginRequest;
import com.example.eCommerce.user.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Public endpoints. Google sign-in is not here: the browser navigates to /oauth2/authorization/google
 * and Spring Security handles the rest (see OAuth2LoginSuccessHandler).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Sign up, log in and reactivate with email + password")
public class AuthController
{
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a local account and return an access token")
    public AuthResponse register(@RequestBody @Valid CreateUserRequest createUserRequest)
    {
        return authService.register(createUserRequest);
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange email + password for an access token")
    public AuthResponse login(@RequestBody @Valid UserLoginRequest userLoginRequest)
    {
        return authService.login(userLoginRequest);
    }

    @PostMapping("/reactivate")
    @Operation(summary = "Reactivate a deactivated local account and return an access token")
    public AuthResponse reactivate(@RequestBody @Valid UserLoginRequest userLoginRequest)
    {
        return authService.reactivate(userLoginRequest);
    }
}
