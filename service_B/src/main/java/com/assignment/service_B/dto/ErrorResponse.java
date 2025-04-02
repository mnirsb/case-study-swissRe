package com.assignment.service_B.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error response structure for Service B")
public class ErrorResponse {
    @Schema(description = "Error code", example = "SERVICE_B_ERROR")
    private String errorCode;

    @Schema(description = "Error message", example = "Processing failed")
    private String message;

    @Schema(description = "Timestamp of the error in milliseconds", example = "1698765432100")
    private long timestamp;
}