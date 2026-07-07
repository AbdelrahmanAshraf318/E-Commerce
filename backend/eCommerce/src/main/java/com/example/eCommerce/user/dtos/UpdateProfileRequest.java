package com.example.eCommerce.user.dtos;

import com.example.eCommerce.common.validatePhone.ValidPhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidPhoneNumber(phoneField = "phoneNumber", regionField = "region")
public class UpdateProfileRequest
{
    @NotBlank
    private String name;

    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String email;

    private String region;
    private String phoneNumber;
}
