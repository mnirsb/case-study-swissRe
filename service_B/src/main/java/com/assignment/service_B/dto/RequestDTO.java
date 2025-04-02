package com.assignment.service_B.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request data for processing in Service B")
public class RequestDTO {
    @NotBlank(message = "Request ID cannot be blank")
    @Size(max = 50, message = "Request ID must be less than 50 characters")
    @Schema(description = "Unique identifier for the request", example = "req123", required = true)
    private String requestId;

    @NotNull(message = "User ID cannot be null")
    @Schema(description = "Identifier of the user making the request", example = "user456", required = true)
    private String userId;

    @NotBlank(message = "Payload cannot be blank")
    @Schema(description = "Payload data for the request", example = "Order details", required = true)
    private String payload;
}