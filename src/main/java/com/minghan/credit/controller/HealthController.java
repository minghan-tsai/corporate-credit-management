package com.minghan.credit.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minghan.credit.service.HealthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

// REST Controller，透過 Constructor Injection 使用 HealthService
@RestController
@Tag(name = "Health")

public class HealthController {

    private final HealthService healthService;

    // Constructor Injection：由 Spring 注入 HealthService Bean
    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    // Health Check Endpoint：允許外部以 GET /api/health 檢查服務狀態
    @GetMapping("/api/health")
    @Operation(summary = "Health check", description = "Returns the current API service health status.")
    @ApiResponse(responseCode = "200", description = "Service is healthy", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    public String getHealth() {
        return healthService.getStatus();
    }
}
