package com.example.eCommerce.user.controller;

import com.example.eCommerce.user.dtos.ChangePasswordRequest;
import com.example.eCommerce.user.dtos.CustomerProfileResponse;
import com.example.eCommerce.user.dtos.UpdateProfileRequest;
import com.example.eCommerce.user.services.CustomerService;
import com.example.eCommerce.user.utils.AuthenticationUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "Customer API")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController
{
    private final CustomerService customerService;

    @GetMapping("/me")
    public CustomerProfileResponse getProfile(Authentication authentication)
    {
        return customerService.getProfile(AuthenticationUtil.getUserId(authentication));
    }

    // Returns the updated profile so the client does not need a second GET.
    @PatchMapping("/me")
    public CustomerProfileResponse updateCustomer
            (
            @RequestBody @Valid UpdateProfileRequest updateProfileRequest,
            Authentication authentication
            )
    {
        return customerService.updateProfileInfo(updateProfileRequest, AuthenticationUtil.getUserId(authentication));
    }

    @PostMapping("/me/password")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void changePassword(@RequestBody @Valid  ChangePasswordRequest changePasswordRequest, Authentication authentication)
    {
        customerService.changePassword(changePasswordRequest, AuthenticationUtil.getUserId(authentication));
    }

    // Reactivation is POST /api/v1/auth/reactivate: once deactivated, the user can no longer authenticate here.
    @PatchMapping("/me/deactivate")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void deactivateAccount(Authentication authentication)
    {
        customerService.deactivateAccount(AuthenticationUtil.getUserId(authentication));
    }

    @DeleteMapping("/me")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void deleteAccount(Authentication authentication)
    {
        customerService.deleteAccount(AuthenticationUtil.getUserId(authentication));
    }
}
