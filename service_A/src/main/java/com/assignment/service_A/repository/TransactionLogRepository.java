package com.assignment.service_A.repository;

import com.assignment.service_A.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    Optional<TransactionLog> findByRequestId(String requestId);

    boolean existsByRequestId(String requestId);

    @Query("SELECT t FROM TransactionLog t WHERE t.status IN :statuses ORDER BY t.updatedAt ASC")
    List<TransactionLog> findByStatusIn(@Param("statuses") List<String> statuses);

    @Query(value = "SELECT * FROM transaction_log ORDER BY updated_at DESC LIMIT :limit", nativeQuery = true)
    List<TransactionLog> findLatestTransactions(@Param("limit") int limit);

    long countByStatus(String status);
}
