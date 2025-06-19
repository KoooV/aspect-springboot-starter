package org.example.aspectspringbootstarter.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import org.example.aspectspringbootstarter.interfaceToMainProject.CacheConfig;
import org.example.aspectspringbootstarter.interfaceToMainProject.CacheStore;
import org.springframework.stereotype.Component;


import java.util.Arrays;
import java.util.Optional;


@Aspect
@Component
@RequiredArgsConstructor
public class CacheAspect {
    private final CacheStore cacheStore;
    private final CacheConfig cacheConfig;
    @Around("@annotation(org.example.aspectspringbootstarter.annotation.Cached)")
    public Object cacheLogic(ProceedingJoinPoint joinPoint) throws Throwable{
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String key = generateKey(signature, joinPoint.getArgs());

        Optional<Object> cachedResult = Optional.ofNullable(cacheStore.get(key));// Optional = null -> Option.empty() или Optional.of(значение)
        if (cachedResult.isPresent()) {
            return cachedResult.get();
        }

        Object result = joinPoint.proceed();
        long ttl = cacheConfig.getTtl().toMillis();//получаем время из yml
        cacheStore.put(key, result, ttl);
        return result;
    }


    public String generateKey(MethodSignature signature, Object[] joinPoint){
        String signaturePart = signature.getDeclaringTypeName() + "." + signature.getMethod();
        String argumentsPart = Arrays.deepToString(joinPoint);//преобразуем массив в строку
        return signaturePart + argumentsPart;
    }
}
