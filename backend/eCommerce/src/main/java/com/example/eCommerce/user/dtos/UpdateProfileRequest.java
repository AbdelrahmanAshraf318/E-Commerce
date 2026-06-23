package com.example.eCommerce.user.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest
{
    private String name;
    private String username;
    private String email;
    private String phoneNumber;
}
