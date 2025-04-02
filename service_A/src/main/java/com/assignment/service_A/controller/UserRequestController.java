package com.assignment.service_A.controller;

import com.assignment.service_A.dto.RequestDTO;
import com.assignment.service_A.dto.ResponseDTO;
import com.assignment.service_A.service.UserRequestService;
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

@RestController
@RequestMapping("/api/user-requests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Request API", description = "API for processing user requests in Service A")
@CrossOrigin("*")
public class UserRequestController {

    private final UserRequestService userRequestService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Process a User Request",
            description = "Submits a user request to Service A, which processes it and interacts with Service B."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Request processed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service unavailable (e.g., Service B down)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))
            )
    })
    public ResponseEntity<ResponseDTO> processRequest(@Valid @RequestBody RequestDTO requestDTO) {
        log.info("Received user request with requestId: {}", requestDTO.getRequestId());
        ResponseDTO response = userRequestService.processRequest(requestDTO);
        log.info("Processed requestId: {} with status: {}", requestDTO.getRequestId(), response.getStatus());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notify-compensation")
    public ResponseEntity<Void> notifyCompensation(@RequestBody ResponseDTO compensation) {
        log.info("Received compensation notification: {}", compensation);
        return ResponseEntity.ok().build();
    }
}