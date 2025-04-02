package com.assignment.service_A.recovery;

import com.assignment.service_A.client.ServiceBClient;
import com.assignment.service_A.dto.RequestDTO;
import com.assignment.service_A.dto.ResponseDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReconciliationService {

    private final ServiceBClient serviceBClient;

    @CircuitBreaker(name = "serviceB", fallbackMethod = "fallbackServiceB")
    @Retry(name = "serviceB")
    public ResponseDTO processRequest(RequestDTO requestDTO) {
        ResponseDTO response = serviceBClient.processTransaction(requestDTO);
        log.debug("Service B response: {}", response);
        return response;
    }

    @Retry(name = "serviceBCompensation", fallbackMethod = "fallbackCompensation")
    public void compensateRequest(String requestId) {
        serviceBClient.compensateTransaction(requestId);
        log.info("Compensation successful for requestId: {}", requestId);
    }

    private void fallbackCompensation(String requestId, Throwable throwable) {
        log.warn("Compensation fallback triggered for requestId: {}. Reason: {}", requestId, throwable.getMessage());
    }

    private ResponseDTO fallbackServiceB(RequestDTO requestDTO, Throwable throwable) {
        log.warn("Fallback triggered for Service B call for requestId: {}. Reason: {}",
                requestDTO.getRequestId(), throwable.getMessage());
        return new ResponseDTO("FALLBACK", requestDTO.getRequestId(), throwable.getMessage());
    }
}