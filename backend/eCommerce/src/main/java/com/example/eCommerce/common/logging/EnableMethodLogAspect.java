package com.example.eCommerce.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Aspect
@Service
@Slf4j
public class EnableMethodLogAspect
{
    @Around("@annotation(EnableMethodLogs)")
    public Object logCurrentMethodData(ProceedingJoinPoint proceedingJoinPoint) throws Throwable
    {
        long initTime = System.currentTimeMillis();

        try
        {
            Object proceed = proceedingJoinPoint.proceed();
            long executionTime = System.currentTimeMillis() - initTime;

            log.info("Method [{}] executed in {}ms | Input: {} | Output: {}",
                    proceedingJoinPoint.getSignature(),
                    executionTime,
                    Arrays.toString(proceedingJoinPoint.getArgs()),
                    proceed);

            return proceed;
        }
        catch (Throwable ex)
        {
            long executionTime = System.currentTimeMillis() - initTime;
            log.error("Method [{}] failed after {}ms | Input: {} | Error: {}",
                    proceedingJoinPoint.getSignature(),
                    executionTime,
                    Arrays.toString(proceedingJoinPoint.getArgs()),
                    ex.getMessage(), ex);
            throw ex;
        }
    }
}
