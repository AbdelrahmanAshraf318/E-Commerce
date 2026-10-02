package com.example.eCommerce.common.validatePhone;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import lombok.experimental.UtilityClass;

import java.util.Locale;

@UtilityClass
public class PhoneNumbers
{

    /**
     * "010 1234 5678" + "EG" -> "+201012345678".
     * Storing one canonical format is what makes the UNIQUE constraint on PHONE_NUMBER meaningful.
     * Call only after {@link ValidPhoneNumber} has passed.
     */
    public String toE164(String phoneNumber, String region)
    {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try
        {
            return phoneUtil.format(phoneUtil.parse(phoneNumber, region.toUpperCase(Locale.ROOT)),
                    PhoneNumberUtil.PhoneNumberFormat.E164);
        }
        catch (NumberParseException e)
        {
            throw new IllegalArgumentException("Phone number was not validated before normalizing", e);
        }
    }
}
