package com.example.eCommerce.user.controller;

import com.example.eCommerce.user.dtos.ChangePasswordRequest;
import com.example.eCommerce.user.dtos.UpdateProfileRequest;
import com.example.eCommerce.user.services.CustomerService;
import com.example.eCommerce.user.utils.AuthenticationUtil;
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
public class CustomerController
{
    private final CustomerService customerService;

    @PatchMapping("/me")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void updateCustomer
            (
            @RequestBody @Valid UpdateProfileRequest updateProfileRequest,
            Authentication authentication
            )
    {
        customerService.updateProfileInfo(updateProfileRequest, AuthenticationUtil.getUserId(authentication));
    }

    @PostMapping("/me/password")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void changePassword(@RequestBody @Valid  ChangePasswordRequest changePasswordRequest, Authentication authentication)
    {
        customerService.changePassword(changePasswordRequest, AuthenticationUtil.getUserId(authentication));
    }

    @PatchMapping("/me/deactivate")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void deactivateAccount(Authentication authentication)
    {
        customerService.deactivateAccount(AuthenticationUtil.getUserId(authentication));
    }

    @PatchMapping("/me/reactivate")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void reactivateAccount(Authentication authentication)
    {
        customerService.reactivateAccount(AuthenticationUtil.getUserId(authentication));
    }

    @DeleteMapping("/me")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void deleteAccount(Authentication authentication)
    {
        customerService.deleteAccount(AuthenticationUtil.getUserId(authentication));
    }
}
