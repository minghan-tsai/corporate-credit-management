package com.minghan.credit.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

// 所有 REST 錯誤共用同一個精簡 contract；欄位驗證明細只在需要時輸出。
public record ApiErrorResponse(

        @Schema(description = "Time when the error occurred", example = "2026-09-18T08:30:00Z") Instant timestamp,

        @Schema(description = "HTTP status code") int status,

        @Schema(description = "HTTP error name") String error,

        @Schema(description = "Error message") String message,

        @Schema(description = "Request path where the error occurred") String path,

        @JsonInclude(JsonInclude.Include.NON_EMPTY) @Schema(description = "Field-level validation errors, returned only when applicable") List<ValidationError> validationErrors

) {

    public ApiErrorResponse {
        validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
    }

    public record ValidationError(

            @Schema(description = "Field that failed validation", example = "requestedAmount") String field,

            @Schema(description = "Validation error message", example = "must be greater than 0") String message

    ) {
    }
}
