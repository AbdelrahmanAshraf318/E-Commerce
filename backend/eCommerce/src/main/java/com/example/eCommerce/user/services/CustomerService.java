package com.example.eCommerce.user.services;

import com.example.eCommerce.user.dtos.ChangePasswordRequest;
import com.example.eCommerce.user.dtos.CustomerProfileResponse;
import com.example.eCommerce.user.dtos.UpdateProfileRequest;
import com.example.eCommerce.user.entity.Customer;

import java.util.UUID;

/**
 * Operations a signed-in customer performs on their own account.
 * Sign-up / login live in {@link AuthService}; Spring Security's user lookup lives in
 * {@link com.example.eCommerce.security.CustomerUserDetailsService}.
 */
public interface CustomerService
{
    Customer getCustomerById(UUID customerId);

    CustomerProfileResponse getProfile(UUID userId);

    CustomerProfileResponse updateProfileInfo(UpdateProfileRequest updateProfileRequest, UUID userId);

    void changePassword(ChangePasswordRequest changePasswordRequest, UUID userId);

    void deactivateAccount(UUID userId);

    void deleteAccount(UUID userId);
}
