package org.example.aspectspringbootstarter.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import org.example.aspectspringbootstarter.interfaceToMainProject.CacheConfigStarter;
import org.example.aspectspringbootstarter.interfaceToMainProject.CacheStoreStarter;
import org.springframework.stereotype.Component;


import java.util.Arrays;
import java.util.Optional;


@Aspect
@Component
@RequiredArgsConstructor
public class CacheAspect {
    private final CacheStoreStarter cacheStoreStarter;
    private final CacheConfigStarter cacheConfigStarter;
    @Around("@annotation(org.example.aspectspringbootstarter.annotation.Cached)")
    public Object cacheLogic(ProceedingJoinPoint joinPoint) throws Throwable{
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String key = generateKey(signature, joinPoint.getArgs());

        Optional<Object> cachedResult = Optional.ofNullable(cacheStoreStarter.get(key));// Optional = null -> Option.empty() или Optional.of(значение)
        if (cachedResult.isPresent()) {
            return cachedResult.get();
        }

        Object result = joinPoint.proceed();
        long ttl = cacheConfigStarter.getTtl().toMillis();//получаем время из yml
        cacheStoreStarter.put(key, result, ttl);
        return result;
    }


    public String generateKey(MethodSignature signature, Object[] joinPoint){
        String signaturePart = signature.getDeclaringTypeName() + "." + signature.getMethod();
        String argumentsPart = Arrays.deepToString(joinPoint);//преобразуем массив в строку
        return signaturePart + argumentsPart;
    }
}
