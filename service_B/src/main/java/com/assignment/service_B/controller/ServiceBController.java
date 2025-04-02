package com.assignment.service_B.controller;

import com.assignment.service_B.dto.ErrorResponse;
import com.assignment.service_B.dto.RequestDTO;
import com.assignment.service_B.dto.ResponseDTO;
import com.assignment.service_B.entity.TransactionLog;
import com.assignment.service_B.repository.TransactionLogRepository;
import com.assignment.service_B.service.ProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/serviceB")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Service B Processing API", description = "API for processing requests and retrieving transaction status")
public class ServiceBController {

    private final ProcessingService processingService;
    private final TransactionLogRepository transactionLogRepository;

    @PostMapping(value = "/process", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Process a Request", description = "Processes a request received from Service A")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request processed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ResponseDTO> processRequest(@Valid @RequestBody RequestDTO requestDTO) {
        log.info("Received request from Service A: {}", requestDTO.getRequestId());
        ResponseDTO response = processingService.processRequest(requestDTO);
        log.info("Processed requestId: {} with status: {}", requestDTO.getRequestId(), response.getStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/status/{requestId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Transaction Status", description = "Retrieves the status of a transaction by request ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)))
    })
    public ResponseEntity<ResponseDTO> getTransactionStatus(@PathVariable String requestId) {
        log.info("Received status check for requestId: {}", requestId);
        Optional<TransactionLog> logOpt = transactionLogRepository.findByRequestId(requestId);
        if (logOpt.isEmpty()) {
            return ResponseEntity.ok(new ResponseDTO("NOT_FOUND", requestId, "Transaction not found"));
        }
        TransactionLog log = logOpt.get();
        return ResponseEntity.ok(new ResponseDTO(log.getStatus(), requestId,
                "Status: " + log.getStatus() + ", Updated: " + log.getUpdatedAt()));
    }
}