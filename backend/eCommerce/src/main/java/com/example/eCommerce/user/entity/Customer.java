package com.example.eCommerce.user.entity;

import com.example.eCommerce.common.validatePhone.ValidPhoneNumber;
import com.example.eCommerce.order.entity.Order;
import com.example.eCommerce.user.role.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;

@Entity
@Table(
        name = "USER",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "USERNAME")
        }
)
@Data
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class) // an AOP to be called before OR after any changing on this entity
@ValidPhoneNumber(phoneField = "phoneNumber", regionField = "region")
public class Customer implements UserDetails
{
    @Id // Mark the field to be primary key
    @GeneratedValue(strategy = GenerationType.UUID)
    @TableGenerator
    @Column(name = "USER_ID")
    private UUID userId;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "USERNAME", unique = true, nullable = false)
    private String username;


    @Column(name = "PASSWORD", nullable = false)
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$",
            message = "Password must be 8-20 characters long and include at least one uppercase letter, one lowercase letter, one digit, and one special character."
    )
    private String password;

    @Email
    @Column(name = "EMAIL", nullable = false, unique = true)
    private String email;

    @DateTimeFormat
    @Column(name = "DATE_OF_BIRTH", nullable = false)
    private Date dateOfBirth;

    @Column(name = "PHONE_NUMBER", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "REGION", nullable = false)
    private String region;

    @Column(name = "IS_LOCKED", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean locked;

    @OneToMany(
            mappedBy = "customer",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Order> orders;

    @ManyToMany(
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.EAGER
    )
    @JoinTable(
            name = "USERS_ROLES",
            joinColumns = {
                    @JoinColumn(name = "USER_ID")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "ROLES_ID")
            }
    )
    private List<Role> roles;

    @Column(name = "IS_ENABLED", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean enabled;

    @Override
    public String getPassword()
    {
        return this.password;
    }

    @Override
    public String getUsername()
    {
        return this.username;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities()
    {
        if(CollectionUtils.isEmpty(this.roles))
            return List.of();

        return this.roles.stream().
                map(role -> new SimpleGrantedAuthority(role.getName()))
                .toList();
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
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled()
    {
        return UserDetails.super.isEnabled();
    }

    @Transient
    public Integer getAge()
    {
        if(Objects.nonNull(this.dateOfBirth))
        {
            LocalDate birthDate = this.dateOfBirth.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            return Period.between(birthDate, LocalDate.now()).getYears();
        }

        return null;
    }
}
