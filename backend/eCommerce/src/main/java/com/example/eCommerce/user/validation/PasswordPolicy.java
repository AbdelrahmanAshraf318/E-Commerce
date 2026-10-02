package com.example.eCommerce.user.validation;

import lombok.experimental.UtilityClass;

/**
 * One definition of the password rule. The two DTOs had diverged: ChangePasswordRequest used an
 * en-dash and an unescaped "[" inside the character class, so it accepted/rejected different passwords.
 */
@UtilityClass
public class PasswordPolicy
{
    public final String REGEX =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()\\-\\[\\]{}:;',?/*~$^+=<>]).{8,20}$";

    public final String MESSAGE =
            "Password must be 8-20 characters long and include at least one uppercase letter, "
                    + "one lowercase letter, one digit, and one special character.";

}
