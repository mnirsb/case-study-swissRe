package com.assignment.service_A.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request data for processing a user request")
public class RequestDTO {
    @Schema(description = "Unique identifier for the request", example = "req123", required = true)
    private String requestId;

    @Schema(description = "Payload data for the request", example = "Order details", required = true)
    private String payload;

    @Schema(description = "Identifier of the user making the request", example = "user456", required = true)
    private String userId;
}