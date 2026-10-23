package com.example.eCommerce.config;

import com.example.eCommerce.user.entity.Customer;
import com.example.eCommerce.user.enums.AccountStatus;
import com.example.eCommerce.user.enums.AuthProvider;
import com.example.eCommerce.user.repository.CustomerRepo;
import com.example.eCommerce.user.repository.RoleRepo;
import com.example.eCommerce.user.role.Role;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;


@Slf4j
@Order(2)
@Service
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner
{

    private final CustomerRepo customerRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String email;
    @Value("${app.admin.password}")
    private String password;
    @Value("${app.admin.name}")
    private String name;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception
    {
        if (customerRepo.existsByRolesName(Role.ADMIN))
            return;
        if (StringUtils.isAnyBlank(email, password))
        {
            log.warn("No admin exists. Set ADMIN_EMAIL and ADMIN_PASSWORD to create one.");
            return;
        }

        Customer admin = new Customer();
        admin.setName(name);
        admin.setEmail(email.trim().toLowerCase(Locale.ROOT));
        admin.setPassword(passwordEncoder.encode(password));
        admin.setAuthProvider(AuthProvider.LOCAL);
        admin.setStatus(AccountStatus.ACTIVE); // added in Step 2
        admin.setRoles(new ArrayList<>(Arrays.asList(
                roleRepo.findByName(Role.ADMIN).orElseThrow(),
                roleRepo.findByName(Role.CUSTOMER).orElseThrow())));

        customerRepo.save(admin);
        log.info("Created the first admin account {}", admin.getEmail());
    }
}
