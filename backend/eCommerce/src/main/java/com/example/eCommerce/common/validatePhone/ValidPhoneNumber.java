package com.example.eCommerce.common.validatePhone;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
public @interface ValidPhoneNumber
{
    String message() default "Invalid phone number for the given region";
    String phoneField();
    String regionField();
    Class<?>[] groups() default {};
    Class<? extends Payload>[]  payload() default {};
}
