package com.example.eCommerce.user.dtos;

import com.example.eCommerce.user.enums.AuthProvider;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * What the API exposes about a customer. Never return the entity itself: it carries the password hash.
 */
public record CustomerProfileResponse(UUID userId,
                                      String name,
                                      String email,
                                      AuthProvider authProvider,
                                      LocalDate dateOfBirth,
                                      Integer age,
                                      String phoneNumber,
                                      String region,
                                      boolean hasPassword,
                                      boolean profileComplete,
                                      List<String> roles)
{
}
