package com.minghan.credit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

// API 回傳 Drawdown 資料；關聯只回傳 ID，避免序列化 Hibernate LAZY Proxy。
public record DrawdownResponse(

        @Schema(description = "Drawdown ID", example = "1") Long id,

        @Schema(description = "Credit limit ID associated with the drawdown", example = "1") Long creditLimitId,

        @Schema(description = "Drawdown amount", example = "1000000.00") BigDecimal amount,

        @Schema(description = "User ID who created the drawdown", example = "2") Long createdById,

        @Schema(description = "Time when the drawdown was created", example = "2026-09-18T15:30:00") LocalDateTime createdAt

) {
}