package com.assignment.service_B.service.impl;

import com.assignment.service_B.dto.RequestDTO;
import com.assignment.service_B.dto.ResponseDTO;
import com.assignment.service_B.entity.TransactionLog;
import com.assignment.service_B.exception.ServiceBExceptions;
import com.assignment.service_B.repository.TransactionLogRepository;
import com.assignment.service_B.service.ProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessingServiceImpl implements ProcessingService {

    private final TransactionLogRepository transactionLogRepository;

    @Transactional
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2),
            include = {DataAccessException.class})
    @Override
    public ResponseDTO processRequest(RequestDTO requestDTO) {
        log.info("Processing request: {}", requestDTO);

        Optional<TransactionLog> existingLog = transactionLogRepository.findByRequestId(requestDTO.getRequestId());

        if (existingLog.isPresent()) {
            log.warn("Duplicate request detected: {}", requestDTO.getRequestId());
            return new ResponseDTO("ALREADY_PROCESSED", requestDTO.getRequestId(), "Duplicate Process stopped! ");
        }

        TransactionLog transactionLog = new TransactionLog();
        transactionLog.setRequestId(requestDTO.getRequestId());
        transactionLog.setStatus("INITIATED");
        transactionLog.setCreatedAt(LocalDateTime.now());
        transactionLog.setUpdatedAt(LocalDateTime.now());
        transactionLogRepository.save(transactionLog);

        try {
            log.info("Processing successful for requestId: {}", requestDTO.getRequestId());
            transactionLog.setStatus("SUCCESS");
            transactionLog.setUpdatedAt(LocalDateTime.now());
            transactionLogRepository.save(transactionLog);
            return new ResponseDTO("SUCCESS", requestDTO.getRequestId(), "Request is successfully completed!");
        } catch (Exception e) {
            log.error("Processing failed for requestId: {}. Error: {}", requestDTO.getRequestId(), e.getMessage());
            transactionLog.setStatus("FAILED");
            transactionLog.setUpdatedAt(LocalDateTime.now());
            transactionLog.setErrorMessage(e.getMessage());
            transactionLogRepository.save(transactionLog);
            throw new ServiceBExceptions("Failed to process request: " + e.getMessage(), e);
        }
    }



    @Transactional
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    @Override
    public void processExistingTransaction(TransactionLog transactionLog) {
        log.info("Recovering transaction: {}", transactionLog.getRequestId());

        try {
            log.info("Recovery processing successful for requestId: {}", transactionLog.getRequestId());

            // Update transaction log to SUCCESS
            transactionLog.setStatus("SUCCESS");
            transactionLog.setUpdatedAt(LocalDateTime.now());
            transactionLogRepository.save(transactionLog);

        } catch (Exception e) {
            log.error("Recovery failed for requestId: {}. Error: {}",
                    transactionLog.getRequestId(), e.getMessage());

            // Mark as FAILED if recovery fails again
            transactionLog.setStatus("FAILED");
            transactionLog.setUpdatedAt(LocalDateTime.now());
            transactionLog.setErrorMessage(e.getMessage());
            transactionLogRepository.save(transactionLog);
        }
    }
}
