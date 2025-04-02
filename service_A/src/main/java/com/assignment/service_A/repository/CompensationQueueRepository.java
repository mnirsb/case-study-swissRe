package com.assignment.service_A.repository;

import com.assignment.service_A.entity.CompensationQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompensationQueueRepository extends JpaRepository<CompensationQueue, Long> {
    List<CompensationQueue> findAll();
}
