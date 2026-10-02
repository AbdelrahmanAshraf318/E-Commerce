package com.example.eCommerce.config;

import com.example.eCommerce.user.repository.RoleRepo;
import com.example.eCommerce.user.role.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Roles are reference data: every sign-up needs ROLE_CUSTOMER to exist.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleSeeder implements ApplicationRunner
{
    private final RoleRepo roleRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args)
    {
        for (String roleName : List.of(Role.CUSTOMER, Role.ADMIN))
        {
            if (roleRepo.findByName(roleName).isEmpty())
            {
                roleRepo.save(Role.builder().name(roleName).build());
                log.info("Created role {}", roleName);
            }
        }
    }
}
