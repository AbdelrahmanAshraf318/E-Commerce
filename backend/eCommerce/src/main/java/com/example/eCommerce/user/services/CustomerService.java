package com.example.eCommerce.user.services;

import com.example.eCommerce.user.dtos.ChangePasswordRequest;
import com.example.eCommerce.user.dtos.CreateUserRequest;
import com.example.eCommerce.user.dtos.UpdateProfileRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.UUID;

public interface CustomerService extends UserDetailsService
{
    @Override
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    void createCustomer(CreateUserRequest createUserRequest)

    void updateProfileInfo(UpdateProfileRequest updateProfileRequest, UUID userId);

    void changePassword(ChangePasswordRequest changePasswordRequest, UUID userId);

    void deactivateAccount(UUID userId);

    void reactivateAccount(UUID userId);

    void deleteAccount(UUID userId);
}
