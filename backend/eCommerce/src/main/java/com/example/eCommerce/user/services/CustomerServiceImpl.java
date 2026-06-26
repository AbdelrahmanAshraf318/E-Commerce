package com.example.eCommerce.user.services;

import com.example.eCommerce.common.enums.ErrorCode;
import com.example.eCommerce.exception.BusinessException;
import com.example.eCommerce.user.dtos.ChangePasswordRequest;
import com.example.eCommerce.user.dtos.UpdateProfileRequest;
import com.example.eCommerce.user.entity.Customer;
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
    private final CustomerMapper customerMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
    {
        return this.customerRepo.findByUserNameIgnoreCase(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, username));
    }

    @Override
    public void updateProfileInfo(UpdateProfileRequest updateProfileRequest, UUID userId)
    {
        Customer customer = customerRepo.findById(userId).
                orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, userId));

        customerMapper.mergeUserInfo(customer, updateProfileRequest);
        customerRepo.save(customer);
    }

    @Override
    public void changePassword(ChangePasswordRequest changePasswordRequest, UUID userId)
    {
        Customer customer = customerRepo.findById(userId).
                orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, userId));
        if(changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmedNewPassword()))
        {
            customerMapper.changePassword(customer, changePasswordRequest);
            customerRepo.save(customer);
        }
        else
            throw new BusinessException(ErrorCode.CONFIRMED_PASSWORD_ERROR);
    }

    @Override
    public void deactivateAccount(UUID userId)
    {
        Customer customer = customerRepo.findById(userId).
                orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, userId));

        if(!customer.isEnabled())
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_DEACTIVATED);

        customer.setEnabled(false);
        customerRepo.save(customer);
    }

    @Override
    public void reactivateAccount(UUID userId)
    {
        Customer customer = customerRepo.findById(userId).
                orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, userId));

        if(customer.isEnabled())
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_ACTIVATED);

        customer.setEnabled(true);
        customerRepo.save(customer);
    }

    @Override
    public void deleteAccount(UUID userId)
    {
        /**
         * Everything under this account MUST be deleted
         */
    }
}
