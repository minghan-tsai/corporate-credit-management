package com.minghan.credit.dto;

import com.minghan.credit.entity.CreditApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// API 回傳授信申請資料。
// 不直接回傳 Company Entity，只回 companyId，避免 Hibernate LAZY Proxy 序列化問題。
public record CreditApplicationResponse(

        @Schema(description = "Credit application ID", example = "1") Long id,

        @Schema(description = "Company ID associated with the credit application", example = "1") Long companyId,

        @Schema(description = "Requested credit amount", example = "5000000.00") BigDecimal requestedAmount,

        @Schema(description = "Purpose of the credit application", example = "Working capital financing") String purpose,

        @Schema(description = "Current status of the credit application") CreditApplicationStatus status,

        @Schema(description = "Time when the credit application was created", example = "2026-09-18T14:30:00") LocalDateTime createdAt

) {
}
