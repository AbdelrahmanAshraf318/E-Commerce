package com.example.eCommerce.user.dtos;

import com.example.eCommerce.common.validatePhone.ValidPhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Used both for normal edits and for the "complete your profile" step after Google sign-in.
 * Null fields are left unchanged. Phone and region must be sent together.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidPhoneNumber(phoneField = "phoneNumber", regionField = "region")
public class UpdateProfileRequest
{
    @NotBlank(message = "Name cannot be empty")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(min = 2, max = 2, message = "Region must be a 2-letter ISO country code, e.g. EG")
    private String region;

    private String phoneNumber;
}
