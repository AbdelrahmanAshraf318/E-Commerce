package com.example.eCommerce.user.services;

import com.example.eCommerce.audit.enums.AuditAction;
import com.example.eCommerce.audit.service.AuditLogService;
import com.example.eCommerce.common.enums.ErrorCode;
import com.example.eCommerce.common.validatePhone.PhoneNumbers;
import com.example.eCommerce.exception.BusinessException;
import com.example.eCommerce.security.JwtService;
import com.example.eCommerce.user.dtos.AuthResponse;
import com.example.eCommerce.user.dtos.CreateUserRequest;
import com.example.eCommerce.user.dtos.UserLoginRequest;
import com.example.eCommerce.user.entity.Customer;
import com.example.eCommerce.user.enums.AuthProvider;
import com.example.eCommerce.user.repository.CustomerRepo;
import com.example.eCommerce.user.repository.RoleRepo;
import com.example.eCommerce.user.role.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService
{
    private final CustomerRepo customerRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public AuthResponse register(CreateUserRequest request)
    {
        String email = normalizeEmail(request.getEmail());
        if (customerRepo.existsByEmailIgnoreCase(email))
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);

        String phoneNumber = PhoneNumbers.toE164(request.getPhoneNumber(), request.getRegion());
        if (customerRepo.existsByPhoneNumber(phoneNumber))
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);

        Customer customer = new Customer();
        customer.setName(request.getName().trim());
        customer.setEmail(email);
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setAuthProvider(AuthProvider.LOCAL);
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setPhoneNumber(phoneNumber);
        customer.setRegion(request.getRegion().toUpperCase(Locale.ROOT));
        customer.setRoles(new ArrayList<>(List.of(customerRole())));

        customerRepo.save(customer);
        auditLogService.record(customer.getUserId(), AuditAction.ACCOUNT_REGISTERED, "Signed up with email");
        log.info("Registered local customer {}", customer.getUserId());
        return issueToken(customer);
    }

    // Not readOnly: a successful login writes an audit entry, and a read-only transaction would never flush it.
    @Override
    @Transactional
    public AuthResponse login(UserLoginRequest request)
    {
        String email = normalizeEmail(request.getEmail());
        try
        {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.getPassword()));
            Customer customer = (Customer) authentication.getPrincipal();
            auditLogService.record(customer.getUserId(), AuditAction.SIGNED_IN, "Email and password");
            return issueToken(customer);
        }
        catch (DisabledException e)
        {
            // Spring checks "disabled" BEFORE the password. Only reveal the account state to someone who knows it.
            if (passwordMatches(email, request.getPassword()))
                throw new BusinessException(ErrorCode.ACCOUNT_DEACTIVATED);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        catch (LockedException e)
        {
            if (passwordMatches(email, request.getPassword()))
                throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        catch (BadCredentialsException e)
        {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Override
    @Transactional
    public AuthResponse reactivate(UserLoginRequest request)
    {
        Customer customer = customerRepo.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .filter(found -> StringUtils.isNotEmpty(found.getPassword()))
                .filter(found -> passwordEncoder.matches(request.getPassword(), found.getPassword()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (customer.isLocked())
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        if (customer.isEnabled())
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_ACTIVATED);

        customer.setEnabled(true);
        auditLogService.record(customer.getUserId(), AuditAction.ACCOUNT_REACTIVATED, "Email and password");
        log.info("Reactivated customer {}", customer.getUserId());
        return issueToken(customer);
    }

    @Override
    @Transactional
    public String loginWithOAuth2(AuthProvider provider, String email, String name, boolean emailVerified)
    {
        // We link accounts by email, so we must trust that the provider verified the user owns it.
        // Otherwise anyone could register a provider account with someone else's address and take over their account.
        if (StringUtils.isBlank(email) || !emailVerified)
            throw new BusinessException(ErrorCode.OAUTH2_EMAIL_NOT_VERIFIED, provider);

        String normalizedEmail = normalizeEmail(email);
        Customer customer = customerRepo.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> createOAuth2Customer(provider, normalizedEmail, name));

        if (customer.isLocked())
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);

        // Signing in with the provider proves account ownership the same way a password does,
        // so it also reactivates a deactivated account (the local equivalent is POST /auth/reactivate).
        if (!customer.isEnabled())
        {
            customer.setEnabled(true);
            auditLogService.record(customer.getUserId(), AuditAction.ACCOUNT_REACTIVATED, "Signed in with " + providerName(provider));
            log.info("Reactivated customer {} via {}", customer.getUserId(), provider);
        }

        auditLogService.record(customer.getUserId(), AuditAction.SIGNED_IN, providerName(provider));

        return jwtService.generateToken(customer);
    }

    private Customer createOAuth2Customer(AuthProvider provider, String email, String name)
    {
        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setName(StringUtils.defaultIfBlank(name, StringUtils.substringBefore(email, "@")));
        customer.setAuthProvider(provider);
        customer.setRoles(new ArrayList<>(List.of(customerRole())));

        customerRepo.save(customer);
        auditLogService.record(customer.getUserId(), AuditAction.ACCOUNT_REGISTERED, "Signed up with " + providerName(provider));
        log.info("Created {} customer {} on first sign-in", provider, customer.getUserId());
        return customer;
    }

    private AuthResponse issueToken(Customer customer)
    {
        return AuthResponse.bearer(jwtService.generateToken(customer), jwtService.getExpirationSeconds());
    }

    private boolean passwordMatches(String email, String rawPassword)
    {
        return customerRepo.findByEmailIgnoreCase(email)
                .map(Customer::getPassword)
                .filter(hash -> passwordEncoder.matches(rawPassword, hash))
                .isPresent();
    }

    private Role customerRole()
    {
        return roleRepo.findByName(Role.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException(Role.CUSTOMER + " is missing - RoleSeeder did not run"));
    }

    /** GOOGLE -> "Google", for human-readable audit details. */
    private static String providerName(AuthProvider provider)
    {
        return StringUtils.capitalize(provider.name().toLowerCase(Locale.ROOT));
    }

    private static String normalizeEmail(String email)
    {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
