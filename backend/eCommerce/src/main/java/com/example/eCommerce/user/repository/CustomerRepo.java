package com.example.eCommerce.user.repository;

import com.example.eCommerce.user.entity.Customer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepo extends JpaRepository<Customer, UUID>
{
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndUserIdNot(String phoneNumber, UUID userId);

    // Roles are LAZY; load them in the same query whenever we build an Authentication from the customer.
    @EntityGraph(attributePaths = "roles")
    Optional<Customer> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<Customer> findWithRolesByUserId(UUID userId);
}
