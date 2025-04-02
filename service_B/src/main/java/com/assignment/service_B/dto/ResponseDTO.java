package com.assignment.service_B.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response data from Service B operations")
public class ResponseDTO {
    @Schema(description = "Status of the operation", example = "SUCCESS",
            allowableValues = {"SUCCESS", "FAILED", "ALREADY_PROCESSED", "NOT_FOUND", "COMPENSATED", "ALREADY_COMPENSATED"})
    private String status;

    @Schema(description = "Unique identifier of the request", example = "req123")
    private String requestId;

    @Schema(description = "Message describing the outcome", example = "Processed successfully")
    private String message;

}