package org.example.aspectspringbootstarter.repository;

import org.example.aspectspringbootstarter.pojo.DataSourceErrorLogPojo;

public interface DataSourceErrorLogRepositorySaver {
    void saveError(DataSourceErrorLogPojo errorLog);
}
