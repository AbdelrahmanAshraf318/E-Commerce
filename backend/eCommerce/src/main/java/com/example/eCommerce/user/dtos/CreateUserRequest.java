package com.example.eCommerce.user.dtos;

import com.example.eCommerce.common.ValidPhoneNumber;
import com.example.eCommerce.user.role.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest
{
    private String name;
    private String username;
    private String password;
    private String email;
    private Date dateOfBirth;
    @ValidPhoneNumber()
    private String phoneNumber;
    private List<Role> roles;
}
