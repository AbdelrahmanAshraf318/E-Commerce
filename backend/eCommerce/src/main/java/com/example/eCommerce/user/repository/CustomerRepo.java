package com.example.eCommerce.user.repository;

import com.example.eCommerce.user.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepo extends JpaRepository<Customer, UUID>
{
    boolean existsByEmail(String email);

    Optional<Customer> findByEmailIgnoreCase(String email);

    Optional<Customer> findByUsernameIgnoreCase(String username);
}
