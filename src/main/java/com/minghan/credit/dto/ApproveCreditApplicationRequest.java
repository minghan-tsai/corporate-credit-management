package com.minghan.credit.dto;

import java.math.BigDecimal;

// API 核准授信申請時的輸入資料。
// record 只承載不可變的輸入資料；金額與 Application 的跨資料規則由 Service 驗證。
public record ApproveCreditApplicationRequest(
        BigDecimal approvedAmount,
        String comment
) {
}
