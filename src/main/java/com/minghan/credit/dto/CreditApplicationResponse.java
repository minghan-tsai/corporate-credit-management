package com.minghan.credit.dto;

import com.minghan.credit.entity.CreditApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// API 回傳授信申請資料。
// 不直接回傳 Company Entity，只回 companyId，避免 Hibernate LAZY Proxy 序列化問題。
public record CreditApplicationResponse(
        Long id,
        Long companyId,
        BigDecimal requestedAmount,
        String purpose,
        CreditApplicationStatus status,
        LocalDateTime createdAt
) {
}