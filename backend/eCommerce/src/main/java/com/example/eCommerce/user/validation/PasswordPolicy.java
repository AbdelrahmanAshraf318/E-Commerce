package com.example.eCommerce.user.validation;

/**
 * One definition of the password rule. The two DTOs had diverged: ChangePasswordRequest used an
 * en-dash and an unescaped "[" inside the character class, so it accepted/rejected different passwords.
 * Constants are explicitly static (not via Lombok's @UtilityClass) because they are used in annotation
 * values, which javac resolves before Lombok can rewrite the class.
 */
public final class PasswordPolicy
{
    public static final String REGEX =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()\\-\\[\\]{}:;',?/*~$^+=<>]).{8,20}$";

    public static final String MESSAGE =
            "Password must be 8-20 characters long and include at least one uppercase letter, "
                    + "one lowercase letter, one digit, and one special character.";

    private PasswordPolicy()
    {
    }
}
