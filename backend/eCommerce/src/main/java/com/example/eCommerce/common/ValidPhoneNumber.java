package com.example.eCommerce.common;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
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
