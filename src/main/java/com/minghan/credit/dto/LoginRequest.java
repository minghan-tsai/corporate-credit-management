package com.minghan.credit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class LoginRequest {
    @Schema(description = "Username used to authenticate", example = "demo_rm")
    private String username;
    @Schema(description = "Password used to authenticate", example = "your-password", format = "password")
    private String password;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
