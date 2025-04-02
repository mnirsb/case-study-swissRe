package com.assignment.service_A.recovery;

import com.assignment.service_A.client.ServiceBClient;
import com.assignment.service_A.dto.RequestDTO;
import com.assignment.service_A.dto.ResponseDTO;
import com.assignment.service_A.entity.CompensationQueue;
import com.assignment.service_A.entity.TransactionLog;
import com.assignment.service_A.repository.CompensationQueueRepository;
import com.assignment.service_A.repository.TransactionLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecoveryService {

    private final TransactionLogRepository transactionLogRepository;
    private final ServiceBClient serviceBClient;
    private final ReconciliationService reconciliationService;
    private final CompensationQueueRepository compensationQueueRepository;

    @PostConstruct
    public void recoverOnStartup() {
        log.info("Recovering incomplete transactions on startup...");
        List<TransactionLog> incompleteTransactions = transactionLogRepository.findByStatusIn(List.of("INITIATED"));
        recoverTransactions(incompleteTransactions);
    }

    @Scheduled(fixedRate = 5 * 60 * 1000) // Every 5 minutes
    public void periodicRecovery() {
        log.info("Running periodic recovery...");
        List<TransactionLog> incompleteTransactions = transactionLogRepository.findByStatusIn(List.of("INITIATED"));
        recoverTransactions(incompleteTransactions);
        List<CompensationQueue> queued = compensationQueueRepository.findAll();
        for (CompensationQueue queue : queued) {
            reconciliationService.compensateRequest(queue.getRequestId());
            compensationQueueRepository.delete(queue);
        }
    }

    private void recoverTransactions(List<TransactionLog> transactions) {
        if (transactions.isEmpty()) {
            log.info("No incomplete transactions found.");
            return;
        }
        for (TransactionLog transaction : transactions) {
            try {
                log.info("Recovering transaction: {}", transaction.getRequestId());

                ResponseDTO status = serviceBClient.getTransactionStatus(transaction.getRequestId());
                if ("SUCCESS".equals(status.getStatus())) {
                    transaction.setStatus("SUCCESS");
                    transactionLogRepository.save(transaction);
                    continue;
                }

                RequestDTO requestDTO = new RequestDTO(transaction.getRequestId(), "", "");

                ResponseDTO response = reconciliationService.processRequest(requestDTO);

                if ("FALLBACK".equals(response.getStatus()) && transaction.getRetryCount() < 3) {
                    transaction.setRetryCount(transaction.getRetryCount() + 1);
                    transactionLogRepository.save(transaction);
                    continue;
                }

                if ("SUCCESS".equals(response.getStatus())) {
                    transaction.setStatus("SUCCESS");
                } else {
                    transaction.setStatus("FAILED");
                    transaction.setErrorMessage(response.getMessage());
                    reconciliationService.compensateRequest(transaction.getRequestId());
                }
                transaction.setUpdatedAt(LocalDateTime.now());
                transactionLogRepository.save(transaction);
                log.info("Transaction {} recovered.", transaction.getRequestId());
            } catch (Exception e) {
                log.error("Failed to recover transaction {}. Error: {}", transaction.getRequestId(), e.getMessage());
                transaction.setStatus("FAILED");
                transaction.setErrorMessage(e.getMessage());
                transactionLogRepository.save(transaction);
            }
        }
    }
}