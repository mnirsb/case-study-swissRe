package com.assignment.service_B.exception;

public class ServiceBExceptions extends RuntimeException {

    public ServiceBExceptions(String message) {
        super(message);
    }

    public ServiceBExceptions(String message, Throwable cause) {
        super(message, cause);
    }
}