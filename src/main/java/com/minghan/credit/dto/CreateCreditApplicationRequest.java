package com.minghan.credit.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

// API 建立授信申請時的輸入資料。
// 只接收使用者可提供的欄位，status 由後端固定為 DRAFT。
public record CreateCreditApplicationRequest(

        @Schema(description = "Company ID associated with the credit application", example = "1") Long companyId,

        @Schema(description = "Requested credit amount", example = "5000000.00") BigDecimal requestedAmount,

        @Schema(description = "Purpose of the credit application", example = "Working capital financing") String purpose

) {
}