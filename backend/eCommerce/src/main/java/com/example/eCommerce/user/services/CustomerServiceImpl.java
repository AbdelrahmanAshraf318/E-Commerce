package com.example.eCommerce.user.services;

import com.example.eCommerce.audit.enums.AuditAction;
import com.example.eCommerce.audit.service.AuditLogService;
import com.example.eCommerce.common.enums.ErrorCode;
import com.example.eCommerce.common.validatePhone.PhoneNumbers;
import com.example.eCommerce.exception.BusinessException;
import com.example.eCommerce.order.repository.OrderRepo;
import com.example.eCommerce.user.dtos.ChangePasswordRequest;
import com.example.eCommerce.user.dtos.CustomerOrderDto;
import com.example.eCommerce.user.dtos.CustomerProfileResponse;
import com.example.eCommerce.user.dtos.UpdateProfileRequest;
import com.example.eCommerce.user.entity.Customer;
import com.example.eCommerce.user.repository.CustomerRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService
{
    private final CustomerRepo customerRepo;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;
    private final OrderRepo orderRepo;
    private final AuditLogService auditLogService;

    /**
     * Returns a lazy reference (no SELECT) - meant for linking another entity to the customer, e.g. a new Cart.
     * Only valid inside the caller's transaction, and only for ids known to exist (the authenticated user).
     */
    @Override
    public Customer getCustomerById(UUID customerId)
    {
        return customerRepo.getReferenceById(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(UUID userId)
    {
        return customerMapper.toProfileResponse(findCustomer(userId));
    }

    @Override
    @Transactional
    public CustomerProfileResponse updateProfileInfo(UpdateProfileRequest updateProfileRequest, UUID userId)
    {
        Customer customer = findCustomer(userId);

        if (StringUtils.isNotBlank(updateProfileRequest.getPhoneNumber()))
        {
            String phoneNumber = PhoneNumbers.toE164(updateProfileRequest.getPhoneNumber(), updateProfileRequest.getRegion());
            if (customerRepo.existsByPhoneNumberAndUserIdNot(phoneNumber, userId))
                throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        String oldName = customer.getName();
        LocalDate oldDateOfBirth = customer.getDateOfBirth();
        String oldPhoneNumber = customer.getPhoneNumber();

        customerMapper.mergeUserInfo(customer, updateProfileRequest);

        List<String> changed = new ArrayList<>();
        if (!Objects.equals(oldName, customer.getName())) changed.add("name");
        if (!Objects.equals(oldDateOfBirth, customer.getDateOfBirth())) changed.add("date of birth");
        if (!Objects.equals(oldPhoneNumber, customer.getPhoneNumber())) changed.add("phone number");
        // Saving the form without changing anything is not an event worth recording.
        if (!changed.isEmpty())
            auditLogService.record(userId, AuditAction.PROFILE_UPDATED, "Changed " + String.join(", ", changed));

        // No save() needed: the entity is managed, so changes are flushed when the transaction commits.
        return customerMapper.toProfileResponse(customer);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest changePasswordRequest, UUID userId)
    {
        Customer customer = findCustomer(userId);

        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmedNewPassword()))
            throw new BusinessException(ErrorCode.CONFIRMED_PASSWORD_ERROR);

        // Google-only accounts have no password yet and may set a first one. Everyone else must prove they know
        // the current one - otherwise a stolen token is enough to take over the account permanently.
        if (StringUtils.isNotEmpty(customer.getPassword()))
        {
            if (StringUtils.isEmpty(changePasswordRequest.getCurrentPassword()))
                throw new BusinessException(ErrorCode.CURRENT_PASSWORD_REQUIRED);

            // BCrypt hashes are salted: encode(x) differs every call, so it can never be compared with equals().
            if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), customer.getPassword()))
                throw new BusinessException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }

        boolean firstPassword = StringUtils.isEmpty(customer.getPassword());
        customer.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        auditLogService.record(userId, AuditAction.PASSWORD_CHANGED, firstPassword ? "First password set" : null);
        log.info("Password changed for customer {}", userId);
    }

    @Override
    @Transactional
    public void deactivateAccount(UUID userId)
    {
        Customer customer = findCustomer(userId);

        if (!customer.isEnabled())
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_DEACTIVATED);

        customer.setEnabled(false);
        auditLogService.record(userId, AuditAction.ACCOUNT_DEACTIVATED);
        log.info("Deactivated customer {}", userId);
    }

    @Override
    @Transactional
    public void deleteAccount(UUID userId)
    {
        // Orders cascade (CascadeType.ALL + orphanRemoval on Customer.orders); USERS_ROLES rows are removed by Hibernate;
        // the cart is removed by the database (ON DELETE CASCADE on CART.CUSTOMER_ID).
        customerRepo.delete(findCustomer(userId));
        // Kept on purpose: AUDIT_LOG has no foreign key to CUSTOMER, so the trail outlives the account.
        auditLogService.record(userId, AuditAction.ACCOUNT_DELETED);
        log.info("Deleted customer {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerOrderDto> getAllOrders(UUID userId)
    {
        // No orders is a normal state: an empty list, never null and never an error.
        return customerMapper.toCustomerOrderDto(orderRepo.findByCustomerUserIdOrderByCreatedAtDesc(userId));
    }

    private Customer findCustomer(UUID userId)
    {
        return customerRepo.findWithRolesByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, userId));
    }
}
