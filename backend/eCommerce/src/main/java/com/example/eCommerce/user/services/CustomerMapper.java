package com.example.eCommerce.user.services;

import com.example.eCommerce.common.validatePhone.PhoneNumbers;
import com.example.eCommerce.user.dtos.CustomerProfileResponse;
import com.example.eCommerce.user.dtos.UpdateProfileRequest;
import com.example.eCommerce.user.entity.Customer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

/**
 * Pure mapping only. The password check moved to CustomerServiceImpl - it is a business rule, not mapping.
 */
@Service
public class CustomerMapper
{
    public void mergeUserInfo(Customer customer, UpdateProfileRequest updateProfileRequest)
    {
        if (Objects.isNull(customer) || Objects.isNull(updateProfileRequest))
            return;

        // The old customer.getRegion().equals(...) threw a NullPointerException for Google users,
        // who start with no region/phone. Plain "set if provided" is enough.
        if (StringUtils.isNotBlank(updateProfileRequest.getName()))
            customer.setName(updateProfileRequest.getName().trim());

        if (Objects.nonNull(updateProfileRequest.getDateOfBirth()))
            customer.setDateOfBirth(updateProfileRequest.getDateOfBirth());

        // @ValidPhoneNumber guarantees phone and region are either both present and valid, or both absent.
        if (StringUtils.isNotBlank(updateProfileRequest.getPhoneNumber()))
        {
            customer.setRegion(updateProfileRequest.getRegion().toUpperCase(Locale.ROOT));
            customer.setPhoneNumber(PhoneNumbers.toE164(updateProfileRequest.getPhoneNumber(), updateProfileRequest.getRegion()));
        }
    }

    public CustomerProfileResponse toProfileResponse(Customer customer)
    {
        return new CustomerProfileResponse(
                customer.getUserId(),
                customer.getName(),
                customer.getEmail(),
                customer.getAuthProvider(),
                customer.getDateOfBirth(),
                customer.getAge(),
                customer.getPhoneNumber(),
                customer.getRegion(),
                StringUtils.isNotEmpty(customer.getPassword()),
                customer.isProfileComplete(),
                customer.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
        );
    }
}
