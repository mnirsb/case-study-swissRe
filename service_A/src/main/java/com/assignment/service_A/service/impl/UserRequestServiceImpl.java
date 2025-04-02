package com.assignment.service_A.service.impl;


import com.assignment.service_A.client.ServiceBClient;
import com.assignment.service_A.dto.RequestDTO;
import com.assignment.service_A.dto.ResponseDTO;
import com.assignment.service_A.entity.CompensationQueue;
import com.assignment.service_A.entity.TransactionLog;
import com.assignment.service_A.recovery.ReconciliationService;
import com.assignment.service_A.repository.CompensationQueueRepository;
import com.assignment.service_A.repository.TransactionLogRepository;
import com.assignment.service_A.service.UserRequestService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRequestServiceImpl implements UserRequestService {

    private final TransactionLogRepository transactionLogRepository;
    private final ReconciliationService reconciliationService;
    private final ServiceBClient serviceBClient;
    private final CompensationQueueRepository compensationQueueRepository;

    @Override
    public ResponseDTO processRequest(RequestDTO requestDTO) {
        // Service A implements a choreography-based Saga, driving the process and triggering compensation if Service B fails.
        TransactionLog transactionLog = createAndSaveInitialLog(requestDTO);

        try {
            CompletableFuture<ResponseDTO> futureResponse = callServiceBAsync(requestDTO);
            ResponseDTO response = futureResponse.get(10, TimeUnit.SECONDS); // Timeout
            updateLogSuccess(transactionLog, response);
            response.setMessage("Successfully processed for requestId: " + requestDTO.getRequestId());
            return response;
        } catch (Exception e) {
            log.error("Failed to process requestId: {}. Error: {}", requestDTO.getRequestId(), e.getMessage());
            updateLogFailed(transactionLog, e);
            triggerCompensation(requestDTO.getRequestId());
            throw new RuntimeException("Service B is unavailable: " + e.getMessage(), e);
        }
    }

    @Async("taskExecutor")
    private CompletableFuture<ResponseDTO> callServiceBAsync(RequestDTO requestDTO) {
        ResponseDTO response = serviceBClient.processTransaction(requestDTO);
        return CompletableFuture.completedFuture(response);
    }

    @Transactional
    private TransactionLog createAndSaveInitialLog(RequestDTO requestDTO) {
        TransactionLog transactionLog = new TransactionLog();
        transactionLog.setRequestId(requestDTO.getRequestId());
        transactionLog.setStatus("INITIATED");
        transactionLog.setCreatedAt(LocalDateTime.now());
        transactionLog.setUpdatedAt(LocalDateTime.now());
        return transactionLogRepository.save(transactionLog);
    }

    @Transactional
    private void updateLogSuccess(TransactionLog transactionLog, ResponseDTO response) {
        transactionLog.setStatus("SUCCESS");
        transactionLog.setUpdatedAt(LocalDateTime.now());
        transactionLogRepository.save(transactionLog);
    }

    @Transactional
    private void updateLogFailed(TransactionLog transactionLog, Exception e) {
        transactionLog.setStatus("FAILED");
        transactionLog.setUpdatedAt(LocalDateTime.now());
        transactionLog.setErrorMessage(e.getMessage());
        transactionLogRepository.save(transactionLog);
    }

    private void triggerCompensation(String requestId) {
        try {
            reconciliationService.compensateRequest(requestId);
        } catch (Exception ex) {
            CompensationQueue queueEntry = new CompensationQueue(requestId);
            compensationQueueRepository.save(queueEntry);
            log.error("Compensation failed for requestId: {}. Error: {}", requestId, ex.getMessage());
        }
    }
}
