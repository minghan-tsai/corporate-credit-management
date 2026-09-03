package com.minghan.credit.dto;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

public class CreateCompanyRequest {
    @NotBlank
    private String name;
    // 台灣公司統一編號固定 8 碼
    @NotBlank
    @Size(min = 8, max = 8)
    private String taxId;

    public String getName() {
        return name;
    }

    public String getTaxId() {
        return taxId;
    }
}
