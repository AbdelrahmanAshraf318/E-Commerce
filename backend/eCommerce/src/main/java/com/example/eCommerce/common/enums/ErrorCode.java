package com.example.eCommerce.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode
{

    USER_NOT_FOUND("USER_NOT_FOUND", "USER_NOT_FOUND with id %s", HttpStatus.NOT_FOUND),
    CONFIRMED_PASSWORD_ERROR("New Password is not equal Confirmed Password", "New Password is not equal Confirmed Password", HttpStatus.BAD_REQUEST),
    ACCOUNT_ALREADY_DEACTIVATED("Account Already Deactivated", "Account Already Deactivated", HttpStatus.BAD_REQUEST),
    ACCOUNT_ALREADY_ACTIVATED("Account Already Activated", "Account Already Activated", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;
}
