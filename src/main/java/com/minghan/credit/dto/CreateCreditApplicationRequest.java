package com.minghan.credit.dto;
import java.math.BigDecimal;

// API 建立授信申請時的輸入資料。
// 只接收使用者可提供的欄位，status 由後端固定為 DRAFT。
public record CreateCreditApplicationRequest(
        Long companyId,
        BigDecimal requestedAmount,
        String purpose
) {
}
