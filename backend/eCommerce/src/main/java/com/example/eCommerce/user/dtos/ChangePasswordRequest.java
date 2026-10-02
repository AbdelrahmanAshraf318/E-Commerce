package com.example.eCommerce.user.dtos;

import com.example.eCommerce.user.validation.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest
{
    // Required when the account already has a password. Google-only accounts can set a first password without it.
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    private String confirmedNewPassword;

    @Override
    public String toString()
    {
        return "ChangePasswordRequest(***)";
    }
}
