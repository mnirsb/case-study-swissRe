package com.assignment.service_A.exception;

public class ServiceAExceptions extends RuntimeException {

    public ServiceAExceptions(String message) {
        super(message);
    }

    public ServiceAExceptions(String message, Throwable cause) {
        super(message, cause);
    }
}