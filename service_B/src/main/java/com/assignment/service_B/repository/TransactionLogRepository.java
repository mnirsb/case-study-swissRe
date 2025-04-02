package com.assignment.service_B.repository;

import com.assignment.service_B.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    Optional<TransactionLog> findByRequestId(String requestId);

    List<TransactionLog> findByStatusIn(List<String> statuses);
}