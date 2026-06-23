package com.example.eCommerce.user.role;

import com.example.eCommerce.common.BaseEntity;
import com.example.eCommerce.user.entity.Customer;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(
        name = "ROLE"
)
@Data
@NoArgsConstructor
@SuperBuilder
public class Role extends BaseEntity
{
    private String name;

    @ManyToMany(mappedBy = "roles")
    private List<Customer> customers;

}
