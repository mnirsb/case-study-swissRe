package com.assignment.service_A.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_logs")
@Data
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 50)
    private String requestId;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // e.g., "INITIATED", "COMPLETED", "FAILED"

    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // Optional: Additional context for the transaction

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;  // To store error details

    @Column(name = "retry_count", nullable = false, columnDefinition = "int default 0")
    private int retryCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Custom status enum (optional)
    public enum Status {
        INITIATED, COMPLETED, FAILED
    }
}