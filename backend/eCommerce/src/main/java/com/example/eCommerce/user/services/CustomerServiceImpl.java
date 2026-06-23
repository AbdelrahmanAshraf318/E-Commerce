package com.example.eCommerce.user.services;

import com.example.eCommerce.user.dtos.ChangePasswordRequest;
import com.example.eCommerce.user.dtos.UpdateProfileRequest;
import com.example.eCommerce.user.repository.CustomerRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService
{
    private final CustomerRepo customerRepo;
    private final PasswordEncoder passwordEncoder;
    private final CustomerMapper customerMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
    {
        return this.customerRepo.findByUserNameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found With username: " + username));
    }

    @Override
    public void updateProfileInfo(UpdateProfileRequest updateProfileRequest, UUID userId) {

    }

    @Override
    public void changePassword(ChangePasswordRequest changePasswordRequest, UUID userId) {

    }

    @Override
    public void deactivateAccount(UUID userId) {

    }

    @Override
    public void reactivateAccount(UUID userId) {

    }

    @Override
    public void deleteAccount(UUID userId) {

    }
}
