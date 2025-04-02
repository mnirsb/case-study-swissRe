package com.assignment.service_B.service;

import com.assignment.service_B.entity.TransactionLog;
import com.assignment.service_B.repository.TransactionLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Recovery service to handle incomplete or failed transactions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecoveryService {

    private final TransactionLogRepository transactionLogRepository;
    private final ProcessingService processingService;
    private final SagaParticipantService sagaParticipantService;

    @PostConstruct
    public void recoverOnStartup() {
        log.info("Recovering incomplete transactions on service startup...");
        recoverTransactions();
    }

    private void recoverTransactions() {
        List<TransactionLog> incompleteTransactions =
                transactionLogRepository.findByStatusIn(List.of("INITIATED"));

        if (incompleteTransactions.isEmpty()) {
            log.info("No incomplete transactions found.");
            return;
        }

        for (TransactionLog transaction : incompleteTransactions) {
            try {
                log.info("Recovering transaction: {}", transaction.getRequestId());
                processingService.processExistingTransaction(transaction);
                log.info("Transaction {} successfully recovered.", transaction.getRequestId());
            } catch (Exception e) {
                log.error("Failed to recover transaction {}. Error: {}", transaction.getRequestId(), e.getMessage());
                sagaParticipantService.compensateTransaction(transaction.getRequestId());
            }
        }
    }

    @Async("taskExecutor")
    @Scheduled(fixedRate = 6 * 60 * 1000)
    public void periodicRecovery() {
        log.info("Running periodic recovery of incomplete transactions...");
        recoverTransactions();
    }
}