package com.minghan.credit.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

// 所有 REST 錯誤共用同一個精簡 contract；欄位驗證明細只在需要時輸出。
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<ValidationError> validationErrors) {

    public ApiErrorResponse {
        validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
    }

    public record ValidationError(String field, String message) {
    }
}
