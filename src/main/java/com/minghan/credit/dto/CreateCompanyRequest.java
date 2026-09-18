package com.minghan.credit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCompanyRequest {
    @NotBlank
    @Schema(description = "Company name", example = "Minghan Technology Co., Ltd.")
    private String name;
    // 台灣公司統一編號固定 8 碼
    @NotBlank
    @Size(min = 8, max = 8)
    @Schema(description = "8-digit Taiwan tax ID", example = "12345678")
    private String taxId;

    public String getName() {
        return name;
    }

    public String getTaxId() {
        return taxId;
    }
}
