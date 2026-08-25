package com.minghan.credit.service;

import org.springframework.stereotype.Service;

// Spring 管理的 Service Bean，提供健康檢查邏輯
@Service
public class HealthService {
    public String getStatus() {
        return "OK";
    }

}
