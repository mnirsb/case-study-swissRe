package com.assignment.service_B.controller;

import com.assignment.service_B.dto.ErrorResponse;
import com.assignment.service_B.dto.ResponseDTO;
import com.assignment.service_B.service.SagaParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compensation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Service B Compensation API", description = "API for compensating failed transactions")
public class CompensationController {

    private final SagaParticipantService sagaParticipantService;

    @PostMapping(value = "/{requestId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Compensate a Transaction", description = "Compensates a previously processed transaction by request ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction compensated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request ID or compensation not applicable",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ResponseDTO> compensate(@PathVariable String requestId) {
        log.info("Received compensation request for requestId: {}", requestId);
        ResponseDTO response = sagaParticipantService.compensateTransaction(requestId);
        log.info("Compensation result for requestId: {} - status: {}", requestId, response.getStatus());
        return ResponseEntity.ok(response);
    }
}