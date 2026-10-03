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
 * <p>
 * This is the bridge between Spring Security and our database: Spring Security knows nothing about
 * customers, it only asks a {@link UserDetailsService} "give me the user called X".
 * {@link Customer} implements {@link UserDetails}, so it can be returned directly.
 */
@Service
@RequiredArgsConstructor
public class CustomerUserDetailsService implements UserDetailsService
{
    private final CustomerRepo customerRepo;

    /**
     * The "username" is the email. Must throw UsernameNotFoundException (not a custom exception) so
     * DaoAuthenticationProvider can turn it into a generic "bad credentials" and not reveal which emails exist.
     * <p>
     * Called by {@code DaoAuthenticationProvider} during password login. After this returns, the provider:
     * <ol>
     *   <li>checks the account is not locked / disabled / expired ({@code UserDetails} flags);</li>
     *   <li>compares the submitted password with the stored BCrypt hash via {@code PasswordEncoder.matches}.</li>
     * </ol>
     * Roles are fetched in the same query ({@code @EntityGraph} on the repository) because they are needed
     * right after, when the JWT is generated.
     *
     * @param email the email the user typed (case-insensitive)
     * @return the customer, as Spring Security's {@link UserDetails}
     * @throws UsernameNotFoundException if no customer has this email
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException
    {
        return customerRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * Used by {@link JwtFilter} on every authenticated request: loads the user named in the token and
     * returns them only if they may still use the API.
     * <p>
     * This database lookup is what makes our otherwise stateless JWTs revocable: deactivating, locking or deleting
     * an account takes effect on the very next request, not when the token expires an hour later.
     * The cost is one indexed primary-key query per request.
     *
     * @param userId the "sub" claim of a verified token
     * @return the customer with roles loaded, or empty if they no longer exist, are disabled or are locked
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findActiveUserById(UUID userId)
    {
        return customerRepo.findWithRolesByUserId(userId)
                .filter(Customer::isEnabled)
                .filter(Customer::isAccountNonLocked);
    }
}
