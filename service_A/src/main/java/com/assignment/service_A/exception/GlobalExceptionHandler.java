package com.assignment.service_A.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @Data
    @Schema(description = "Error response structure")
    public static class ErrorResponse {
        @Schema(description = "Error code", example = "SERVICE_UNAVAILABLE")
        private String errorCode;
        @Schema(description = "Error message", example = "Service B is down")
        private String message;
    }

    @ExceptionHandler(ServiceAExceptions.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<ErrorResponse> handleServiceException(ServiceAExceptions ex) {
        ErrorResponse error = new ErrorResponse();
        error.setErrorCode("SERVICE_UNAVAILABLE");
        error.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse();
        error.setErrorCode("INTERNAL_SERVER_ERROR");
        error.setMessage("An unexpected error occurred: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}