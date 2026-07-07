package com.example.eCommerce.user.services;

import com.example.eCommerce.user.dtos.ChangePasswordRequest;
import com.example.eCommerce.user.dtos.UpdateProfileRequest;
import com.example.eCommerce.user.entity.Customer;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomerMapper
{
    private final PasswordEncoder passwordEncoder;

    public void mergeUserInfo(Customer customer, UpdateProfileRequest updateProfileRequest)
    {
        if(Objects.nonNull(customer) && Objects.nonNull(updateProfileRequest))
        {
            if(StringUtils.isNotEmpty(updateProfileRequest.getEmail()) &&
                    !customer.getEmail().equals(updateProfileRequest.getEmail()))
                customer.setEmail(updateProfileRequest.getEmail());

            if(StringUtils.isNotEmpty(updateProfileRequest.getName()) &&
                    !customer.getName().equals(updateProfileRequest.getName()))
                customer.setName(updateProfileRequest.getName());

            if(StringUtils.isNotEmpty(updateProfileRequest.getUsername()) &&
                    !customer.getUsername().equals(updateProfileRequest.getUsername()))
                customer.setUsername(updateProfileRequest.getUsername());

            if(StringUtils.isNotEmpty(updateProfileRequest.getRegion()) &&
            !customer.getRegion().equals(updateProfileRequest.getRegion()))
                customer.setRegion(updateProfileRequest.getRegion());

            if(StringUtils.isNotEmpty(updateProfileRequest.getPhoneNumber()) &&
                    !customer.getPhoneNumber().equals(updateProfileRequest.getPhoneNumber()))
                customer.setPhoneNumber(updateProfileRequest.getPhoneNumber());
        }
    }

    public void changePassword(Customer customer, ChangePasswordRequest changePasswordRequest)
    {
        if(Objects.nonNull(customer) && Objects.nonNull(changePasswordRequest))
        {
            if(StringUtils.isNotEmpty(changePasswordRequest.getCurrentPassword()))
            {
                if(StringUtils.isNotEmpty(customer.getPassword()) &&
                        !customer.getPassword().equals(passwordEncoder.encode(changePasswordRequest.getCurrentPassword())))
                {
                    customer.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
                }
            }
        }
    }
}
