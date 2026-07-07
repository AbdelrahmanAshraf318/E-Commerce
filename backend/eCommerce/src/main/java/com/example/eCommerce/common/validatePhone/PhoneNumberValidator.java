package com.example.eCommerce.common.validatePhone;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Objects;

public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, Object>
{

    private String phoneField;
    private String regionField;

    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation)
    {
        this.phoneField = constraintAnnotation.phoneField();
        this.regionField = constraintAnnotation.regionField();
    }

    @Override
    public boolean isValid(Object dto, ConstraintValidatorContext context)
    {
        if(Objects.nonNull(dto))
        {
            BeanWrapper wrapper = new BeanWrapperImpl(dto);
            Object phoneValue = wrapper.getPropertyValue(phoneField);
            Object regionValue = wrapper.getPropertyValue(regionField);

            if(Objects.isNull(phoneValue) || Objects.isNull(regionValue))
                return false;

            String phoneNumber = phoneValue.toString();
            String region = regionValue.toString();

            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

            try
            {
                var parsedNumber = phoneUtil.parse(phoneNumber, region);
                boolean valid = phoneUtil.isValidNumber(parsedNumber);

                if (!valid)
                {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(
                            "Phone number is not valid for region " + region
                    ).addPropertyNode(phoneField).addConstraintViolation();
                }

                return valid;
            }
            catch (NumberParseException e)
            {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "Phone number could not be parsed for region " + region
                ).addPropertyNode(phoneField).addConstraintViolation();
                return false;
            }
        }
        return false;
    }
}