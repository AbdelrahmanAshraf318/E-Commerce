package com.example.eCommerce.exception;

import com.example.eCommerce.common.enums.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException
{
    private final ErrorCode errorCode;
    private final Object[] args;

    public BusinessException(ErrorCode errorCode, Object... args)
    {
        super(getFormatterMessage(errorCode, args));
        this.errorCode = errorCode;
        this.args = args;
    }

    private static String getFormatterMessage(ErrorCode errorCode, Object[] args)
    {
        // List.of(args) threw a NullPointerException as soon as one argument was null.
        if (args != null && args.length > 0)
            return String.format(errorCode.getDefaultMessage(), args);

        return errorCode.getDefaultMessage();
    }
}
