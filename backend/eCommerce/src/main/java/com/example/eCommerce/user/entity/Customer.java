package com.example.eCommerce.user.entity;

import com.example.eCommerce.order.entity.Order;
import com.example.eCommerce.user.enums.AuthProvider;
import com.example.eCommerce.user.role.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "CUSTOMER")
@Setter
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Customer implements UserDetails
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "USER_ID")
    private UUID userId;

    @Column(name = "NAME", nullable = false)
    private String name;

    // Email is the most reliable unique identifier across OAuth providers.
    @Column(name = "EMAIL", nullable = false, unique = true)
    private String email;

    // Made nullable. OAuth users will not have a password.
    @Column(name = "PASSWORD")
    private String password;

    // Tracks where this user originated (LOCAL, GOOGLE, APPLE)
    @Enumerated(EnumType.STRING)
    @Column(name = "AUTH_PROVIDER", nullable = false)
    private AuthProvider authProvider;

    // Made nullable. You can prompt the user to fill these in later
    // via a "Complete Your Profile" step.
    @Column(name = "DATE_OF_BIRTH")
    private LocalDate dateOfBirth;

    @Column(name = "PHONE_NUMBER", unique = true)
    private String phoneNumber;

    @Column(name = "REGION")
    private String region;

    @Column(name = "IS_LOCKED", nullable = false)
    private boolean locked = false;

    @Column(name = "IS_ENABLED", nullable = false)
    private boolean enabled = true;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();

    // LAZY + @EntityGraph in CustomerRepo when roles are needed.
    // No cascade: roles are reference data and must never be created/changed through a customer.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "USERS_ROLES",
            joinColumns = @JoinColumn(name = "USER_ID"),
            inverseJoinColumns = @JoinColumn(name = "ROLE_ID")
    )
    private List<Role> roles = new ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities()
    {
        if (CollectionUtils.isEmpty(this.roles))
        {
            return List.of();
        }
        return this.roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .toList();
    }

    @Override
    public String getPassword()
    {
        return this.password;
    }

    // Spring Security uses this to identify the user.
    // Email is better suited for this than a separate username field in modern apps.
    @Override
    public String getUsername()
    {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired()
    {
        return true;
    }

    @Override
    public boolean isAccountNonLocked()
    {
        return !this.locked;
    }

    @Override
    public boolean isCredentialsNonExpired()
    {
        return true;
    }

    @Override
    public boolean isEnabled()
    {
        return this.enabled;
    }

    /** Google users start without these; the SPA sends them to "complete profile" until this is true. */
    @Transient
    public boolean isProfileComplete()
    {
        return dateOfBirth != null && phoneNumber != null && region != null;
    }

    @Transient
    public Integer getAge()
    {
        if (this.dateOfBirth != null) {
            return Period.between(this.dateOfBirth, LocalDate.now()).getYears();
        }
        return null;
    }
}