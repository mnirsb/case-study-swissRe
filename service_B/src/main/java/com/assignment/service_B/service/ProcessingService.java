package com.assignment.service_B.service;

import com.assignment.service_B.dto.RequestDTO;
import com.assignment.service_B.dto.ResponseDTO;
import com.assignment.service_B.entity.TransactionLog;


public interface ProcessingService {

    ResponseDTO processRequest(RequestDTO requestDTO);

    void processExistingTransaction(TransactionLog transactionLog);
}