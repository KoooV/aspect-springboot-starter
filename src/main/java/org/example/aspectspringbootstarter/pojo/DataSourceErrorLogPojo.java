package org.example.aspectspringbootstarter.pojo;

public class DataSourceErrorLogPojo {
    private String errorMessage;
    private String stackTrace;
    private String methodSignature;

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public String getStackTrace() {
        return this.stackTrace;
    }

    public String getMethodSignature() {
        return this.methodSignature;
    }
}
