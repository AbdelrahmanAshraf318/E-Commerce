package com.example.eCommerce.user.dtos;

import com.example.eCommerce.common.validatePhone.ValidPhoneNumber;
import com.example.eCommerce.user.role.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidPhoneNumber(phoneField = "phoneNumber", regionField = "region")
public class CreateUserRequest
{
    @NotBlank
    private String name;

    @NotBlank
    private String username;

    @NotBlank
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$",
            message = "Password must be 8-20 characters long and include at least one uppercase letter, one lowercase letter, one digit, and one special character."
    )
    private String password;

    @NotBlank
    @Email
    private String email;

    @DateTimeFormat
    private Date dateOfBirth;
    private String phoneNumber;
    private String region;

    @UniqueElements
    private List<Role> roles;
}
