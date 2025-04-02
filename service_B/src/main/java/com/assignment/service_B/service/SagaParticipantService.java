package com.assignment.service_B.service;

import com.assignment.service_B.dto.ResponseDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.assignment.service_B.repository.TransactionLogRepository;
import com.assignment.service_B.entity.TransactionLog;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaParticipantService {

    private final TransactionLogRepository transactionLogRepository;
    private final RestTemplate restTemplate;

    @Transactional
    public ResponseDTO compensateTransaction(String requestId) {
        Optional<TransactionLog> logOpt = transactionLogRepository.findByRequestId(requestId);

        if (logOpt.isEmpty()) {
            log.warn("No transaction found with ID: {}", requestId);
            return new ResponseDTO("NOT_FOUND", requestId, "Transaction not found");
        }

        TransactionLog Translog = logOpt.get();
        if ("FAILED".equals(Translog.getStatus()) || "COMPENSATED".equals(Translog.getStatus())) {
            log.info("Transaction {} already compensated or failed, skipping.", requestId);
            return new ResponseDTO("ALREADY_COMPENSATED", requestId, "Transaction already compensated");
        }

        Translog.setStatus("COMPENSATED");
        Translog.setUpdatedAt(LocalDateTime.now());
        transactionLogRepository.save(Translog);

        if ("COMPENSATED".equals(Translog.getStatus())) {
            restTemplate.postForEntity("http://localhost:7080/api/notify-compensation",
                    new ResponseDTO("COMPENSATED", requestId, "Compensated by Service B"), Void.class);
        }

        log.info("Transaction {} compensated.", requestId);
        return new ResponseDTO("COMPENSATED", requestId, "Transaction compensated successfully");
    }
}