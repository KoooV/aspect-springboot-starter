package org.example.aspectspringbootstarter.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.aspectspringbootstarter.interfaceToMainProject.MetricConfig;
import org.example.aspectspringbootstarter.pojo.TimeLimitExceedLog;
import org.example.aspectspringbootstarter.repository.TimeLimitExceedLogRepositorySaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Aspect
@RequiredArgsConstructor
public class MetricAspect {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Logger log = LoggerFactory.getLogger(MetricAspect.class);
    private final ObjectMapper objectMapper;
    private final TimeLimitExceedLogRepositorySaver repository;
    private final MetricConfig config;
    private static final String METRIC_TOPIC = "metrics-topic";

    @Around("@annotation(org.example.aspectspringbootstarter.annotation.Metric)")
    public Object measuringTime(ProceedingJoinPoint joinPoint) throws Throwable{
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long totalTime = System.currentTimeMillis() - startTime;//время отработки метода в миллисекундах


        if(totalTime > config.getTimeLimit()){
            try{
                sendToKafka(signature, totalTime);
            }catch(Exception ex){
                log.error("Failed attempt to send a message to Kafka, error is saved in the DB");
                saveToDB(signature);
            }
        }
        return result;//в Around обязательно вернуть результат или метод зависнет
    }

    public void saveToDB(MethodSignature signature){
        TimeLimitExceedLog errorLog = new TimeLimitExceedLog();
        try {
            Map<String, Object> methodInfo = new HashMap();
            methodInfo.put("className", signature.getDeclaringTypeName());//название класса
            methodInfo.put("methodName", signature.getName());// название метода
            methodInfo.put("parameterName", signature.getParameterNames());// название параметров метода
            methodInfo.put("parameterTypes", signature.getParameterTypes());// типы параметров
            errorLog.setError(objectMapper.writeValueAsString(methodInfo));// конвертация в json
            repository.save(errorLog);
        }catch(Exception ex){
            log.error("Couldn't convert to JSON or DB error{}", ex.getMessage());

        }
    }

    public void sendToKafka(MethodSignature signature,long methodTotalTime ) throws JsonProcessingException {
        TimeLimitExceedLog metricLog = new TimeLimitExceedLog();
            Map<String, Object> metricInfo = new HashMap();
            metricInfo.put("className", signature.getDeclaringTypeName());//название класса
            metricInfo.put("methodName", signature.getName());// название метода
            metricInfo.put("parameterName", signature.getParameterNames());// название параметров метода
            metricInfo.put("parameterTypes", signature.getParameterTypes());// типы параметров
            metricInfo.put("totalMethodTime", methodTotalTime);//итоговое время отработки метода
            String jsonMetric = objectMapper.writeValueAsString(metricInfo);// конвертация в json

            try{
                var message = MessageBuilder
                    .withPayload(jsonMetric)
                    .setHeader(KafkaHeaders.TOPIC, METRIC_TOPIC)
                    .setHeader("errorType", "METRICS")
                    .build();
                    
                kafkaTemplate.send(message).get(5, TimeUnit.SECONDS);
                log.info("Metric sent to Kafka topic {} with errorType=METRICS", METRIC_TOPIC);
            }catch(InterruptedException | ExecutionException | TimeoutException e){
                log.error("Failed to sent metrics to Kafka {}", e.getMessage());
            }
    }

}
