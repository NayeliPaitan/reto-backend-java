package com.empresa.inventory.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ControllerLoggingAspect {

    @Around("execution(public * com.empresa.inventory.controller..*(..))")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String operation = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        long start = System.nanoTime();
        log.info("-> {}", operation);
        try {
            Object result = joinPoint.proceed();
            log.info("<- {} OK ({} ms)", operation, elapsedMs(start));
            return result;
        } catch (Throwable ex) {
            log.warn("<- {} FALLO {} ({} ms): {}", operation, ex.getClass().getSimpleName(), elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
