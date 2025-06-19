package org.example.aspectspringbootstarter.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.aspectspringbootstarter.repository.DataSourceErrorLogRepositorySaver;
import org.example.aspectspringbootstarter.pojo.DataSourceErrorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Aspect
@Component
@RequiredArgsConstructor
public class LogDataSourceError {
    private static Logger log = LoggerFactory.getLogger(LogDataSourceError.class);
    private final DataSourceErrorLogRepositorySaver repository;
    private final ObjectMapper objectMapper;// для преобразования в json
    private final KafkaTemplate<String,String> kafkaTemplate;
    private static final String DATASOURCE_TOPIC = "datasource-topic";

    @Around("@annotation(org.example.aspectspringbootstarter.annotation.DataSourceError)")
    public Object logError(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch(Exception e) {
            try {
                sendToKafka(joinPoint, e);
            } catch(Exception kafkaEx) {
                log.error("Failed to send error to Kafka, saving to DB: {}", kafkaEx.getMessage());
                saveException(joinPoint, e);
            }
            throw e;
        }
    }

    public void saveException(ProceedingJoinPoint joinPoint, Exception e) {
        DataSourceErrorLog errorLog = new DataSourceErrorLog();

        try {
            errorLog.setErrorMessage(e.getMessage());//сообщение об ошибке message в бд

            StringWriter source = new StringWriter();// экземпляр для получения стэка в виде строки
            e.printStackTrace(new PrintWriter(source));
            errorLog.setStackTrace(source.toString());// сохраняем накопленный стек в stack_trace бд

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();// формируем json с информацией о методе
            Map<String, Object> methodInfo = new HashMap<>();
            methodInfo.put("className", signature.getDeclaringTypeName());
            methodInfo.put("methodName", signature.getName());
            methodInfo.put("parameterTypes", signature.getParameterTypes());
            methodInfo.put("parameterNames", signature.getParameterNames());
            methodInfo.put("parameterValues", joinPoint.getArgs());
            methodInfo.put("entityType", entityType(signature));
            errorLog.setMethodSignature(objectMapper.writeValueAsString(methodInfo));// to json

            repository.save(errorLog);

            log.error("Method exception {}.{}: {}",
                    signature.getDeclaringTypeName(),//имя класса
                    signature.getName(),//имя метода
                    e.getMessage());//текст ошибки

        } catch(Exception ex) {
            log.error("Error saving the log in the database: {}", ex.getMessage());
        }
    }

    public void sendToKafka(ProceedingJoinPoint joinPoint, Exception e) throws Exception {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Map<String, Object> methodInfo = new HashMap<>();
        methodInfo.put("className", signature.getDeclaringTypeName());
        methodInfo.put("methodName", signature.getName());
        methodInfo.put("parameterTypes", signature.getParameterTypes());
        methodInfo.put("parameterNames", signature.getParameterNames());
        methodInfo.put("parameterValues", joinPoint.getArgs());
        methodInfo.put("entityType", entityType(signature));
        methodInfo.put("errorMessage", e.getMessage());
        String jsonErrorLog = objectMapper.writeValueAsString(methodInfo);

        var message = MessageBuilder
                .withPayload(jsonErrorLog)
                .setHeader(KafkaHeaders.TOPIC, DATASOURCE_TOPIC)
                .setHeader("errorType", "DATA_SOURCE")
                .build();

        kafkaTemplate.send(message).get(5, TimeUnit.SECONDS);
        log.info("Error sent to Kafka topic {} with errorType=DATA_SOURCE", DATASOURCE_TOPIC);
    }

    private Object entityType(MethodSignature signature) {
        String entityName = signature.getDeclaringType().getSimpleName();
        return entityName
                .replace("Repository", "")
                .replace("Controller", "");
    }

}