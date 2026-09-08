package com.minghan.credit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// API 回傳 Drawdown 資料；關聯只回傳 ID，避免序列化 Hibernate LAZY Proxy。
public record DrawdownResponse(
        Long id,
        Long creditLimitId,
        BigDecimal amount,
        Long createdById,
        LocalDateTime createdAt
) {
}
