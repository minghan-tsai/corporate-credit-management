package com.minghan.credit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// API 駁回授信申請時的輸入資料。
// comment 是否有效屬於 Reject Business Rule，因此集中由 Service 驗證。
public record RejectCreditApplicationRequest(

        @Schema(description = "Reason for rejecting the credit application", example = "Insufficient financial information") String comment

) {
}