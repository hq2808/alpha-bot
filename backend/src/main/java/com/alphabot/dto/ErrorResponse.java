package com.alphabot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard Error Response Object")
public class ErrorResponse {

    @Schema(description = "Timestamp of the error", example = "2024-03-03T22:55:21")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP Status Code", example = "400")
    private int status;

    @Schema(description = "Error Title", example = "Bad Request")
    private String error;

    @Schema(description = "Detailed error message", example = "Insufficient cash balance")
    private String message;

    @Schema(description = "Path where the error occurred", example = "/api/portfolio/manual/trade")
    private String path;
}
