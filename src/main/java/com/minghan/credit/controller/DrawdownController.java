package com.minghan.credit.controller;

import com.minghan.credit.dto.CreateDrawdownRequest;
import com.minghan.credit.dto.DrawdownResponse;
import com.minghan.credit.service.DrawdownService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/credit-limits/{creditLimitId}/drawdowns")
public class DrawdownController {

    private final DrawdownService drawdownService;

    public DrawdownController(DrawdownService drawdownService) {
        this.drawdownService = drawdownService;
    }

    // 沿用 Stage 5 method security：只有 RM 可以建立 Drawdown。
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RM')")
    public DrawdownResponse create(
            @PathVariable Long creditLimitId,
            @RequestBody CreateDrawdownRequest request) {
        return drawdownService.create(creditLimitId, request);
    }
}
