package com.example.eCommerce.user.dtos;

import com.example.eCommerce.common.validatePhone.ValidPhoneNumber;
import com.example.eCommerce.user.validation.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidPhoneNumber(phoneField = "phoneNumber", regionField = "region")
public class CreateUserRequest
{
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    // Required for local sign-up. Google users fill these in later through PATCH /api/v1/users/me.
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Region is required")
    @Size(min = 2, max = 2, message = "Region must be a 2-letter ISO country code, e.g. EG")
    private String region;

    // Never let Lombok print the password into logs.
    @Override
    public String toString()
    {
        return "CreateUserRequest(name=" + name + ", email=" + email + ")";
    }
}
