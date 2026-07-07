package com.example.eCommerce.exception;

import com.example.eCommerce.common.enums.ErrorCode;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.util.List;

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
        if(!CollectionUtils.isEmpty(List.of(args)))
            return String.format(errorCode.getDefaultMessage(), args);

        return errorCode.getDefaultMessage();
    }
}
