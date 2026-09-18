package com.minghan.credit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "Login result message", example = "Login successful") String message,

        @Schema(description = "JWT bearer token used for authenticated API requests", example = "eyJhbGciOiJIUzI1NiJ9...") String token) {
}