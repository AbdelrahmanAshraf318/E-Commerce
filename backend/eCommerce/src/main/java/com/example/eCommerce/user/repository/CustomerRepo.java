package com.example.eCommerce.user.repository;

import com.example.eCommerce.user.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepo extends JpaRepository<Customer, UUID>
{
    boolean existByEmailIgnoreCase(String email);

    Optional<Customer> findByEmailIgnoreCase(String email);

    Optional<Customer> findByUserNameIgnoreCase(String username);

    boolean existByPhoneNumber(String phoneNumber);
}
