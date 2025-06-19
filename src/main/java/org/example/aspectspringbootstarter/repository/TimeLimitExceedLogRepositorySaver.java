package org.example.aspectspringbootstarter.repository;

import org.example.aspectspringbootstarter.pojo.TimeLimitExceedLog;

public interface TimeLimitExceedLogRepositorySaver {
    void save(TimeLimitExceedLog errorLog);
}
