package com.example.eCommerce.user.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest
{
    private String currentPassword;
    private String newPassword;
    private String confirmedNewPassword;
}
