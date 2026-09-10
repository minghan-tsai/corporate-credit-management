package com.minghan.credit.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minghan.credit.service.HealthService;

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
    public String getHealth() {
        return healthService.getStatus();
    }
}
