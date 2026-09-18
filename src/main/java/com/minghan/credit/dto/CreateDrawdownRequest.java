package com.minghan.credit.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

// API 建立 Drawdown 時的輸入資料。
// CreditLimit 與建立者由 path 及目前登入使用者決定，不接受呼叫端指定。
public record CreateDrawdownRequest(

        @Schema(description = "Drawdown amount", example = "1000000.00") BigDecimal amount

) {
}