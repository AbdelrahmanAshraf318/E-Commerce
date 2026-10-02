package com.example.eCommerce.common.validatePhone;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Locale;
import java.util.Objects;

/**
 * Validates phone + region together.
 * <p>
 * Both empty is VALID - whether they are required is @NotBlank's job, not this validator's.
 * Returning false for nulls (the old behaviour) broke every request where the phone is optional,
 * e.g. a profile update that only changes the name.
 */
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
        if (Objects.isNull(dto))
            return true;

        BeanWrapper wrapper = new BeanWrapperImpl(dto);
        String phoneNumber = Objects.toString(wrapper.getPropertyValue(phoneField), null);
        String region = Objects.toString(wrapper.getPropertyValue(regionField), null);

        if (StringUtils.isAllBlank(phoneNumber, region))
            return true;

        if (StringUtils.isBlank(phoneNumber) || StringUtils.isBlank(region))
            return violation(context, StringUtils.isBlank(phoneNumber) ? phoneField : regionField,
                    "Phone number and region must be provided together");

        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        String upperRegion = region.toUpperCase(Locale.ROOT);

        if (!phoneUtil.getSupportedRegions().contains(upperRegion))
            return violation(context, regionField, "Unknown region " + region);

        try
        {
            if (!phoneUtil.isValidNumber(phoneUtil.parse(phoneNumber, upperRegion)))
                return violation(context, phoneField, "Phone number is not valid for region " + upperRegion);
            return true;
        }
        catch (NumberParseException e)
        {
            return violation(context, phoneField, "Phone number could not be parsed for region " + upperRegion);
        }
    }

    private static boolean violation(ConstraintValidatorContext context, String field, String message)
    {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addPropertyNode(field).addConstraintViolation();
        return false;
    }
}
