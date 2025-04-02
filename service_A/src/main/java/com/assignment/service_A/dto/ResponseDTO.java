package com.assignment.service_A.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response data after processing a user request")
public class ResponseDTO {
    @Schema(description = "Status of the request", example = "SUCCESS", allowableValues = {"SUCCESS", "FAILED", "ERROR", "FALLBACK"})
    private String status;

    @Schema(description = "Unique identifier of the request", example = "req123")
    private String requestId;

    @Schema(description = "Message describing the outcome", example = "Successfully processed")
    private String message;
}