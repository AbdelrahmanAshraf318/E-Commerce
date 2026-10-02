package com.example.eCommerce.security;

import com.example.eCommerce.user.entity.Customer;
import com.example.eCommerce.user.repository.CustomerRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Kept separate from CustomerService: Spring Security needs this bean to build the
 * AuthenticationManager, and CustomerService/AuthService need the AuthenticationManager.
 * Mixing them is what caused the circular dependency on startup.
 */
@Service
@RequiredArgsConstructor
public class CustomerUserDetailsService implements UserDetailsService
{
    private final CustomerRepo customerRepo;

    /**
     * The "username" is the email. Must throw UsernameNotFoundException (not a custom exception) so
     * DaoAuthenticationProvider can turn it into a generic "bad credentials" and not reveal which emails exist.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException
    {
        return customerRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findActiveUserById(UUID userId)
    {
        return customerRepo.findWithRolesByUserId(userId)
                .filter(Customer::isEnabled)
                .filter(Customer::isAccountNonLocked);
    }
}
