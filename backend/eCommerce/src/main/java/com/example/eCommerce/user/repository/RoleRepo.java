package com.example.eCommerce.user.repository;

import com.example.eCommerce.user.role.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepo extends JpaRepository<Role, UUID>
{
    Optional<Role> findByName(String name);
}
