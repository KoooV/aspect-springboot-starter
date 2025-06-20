package org.example.aspectspringbootstarter.repository;

import org.example.aspectspringbootstarter.pojo.TimeLimitExceedLogPojo;

public interface TimeLimitExceedLogRepositorySaver {
    void saveLimit(TimeLimitExceedLogPojo errorLog);
}
