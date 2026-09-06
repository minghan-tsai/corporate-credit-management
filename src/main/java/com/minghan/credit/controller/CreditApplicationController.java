package com.minghan.credit.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.minghan.credit.dto.ApproveCreditApplicationRequest;
import com.minghan.credit.dto.CreateCreditApplicationRequest;
import com.minghan.credit.dto.CreditApplicationResponse;
import com.minghan.credit.dto.RejectCreditApplicationRequest;
import com.minghan.credit.service.CreditApplicationService;

@RestController
@RequestMapping("/api/credit-applications")
public class CreditApplicationController {

    private final CreditApplicationService creditApplicationService;

    public CreditApplicationController(
            CreditApplicationService creditApplicationService) {
        this.creditApplicationService = creditApplicationService;
    }

    // 建立授信申請，成功後回傳 HTTP 201 Created。
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreditApplicationResponse create(
            @RequestBody CreateCreditApplicationRequest request) {
        return creditApplicationService.create(request);
    }

    // 將指定授信申請由 DRAFT 送審為 SUBMITTED。
    @PostMapping("/{id}/submit")
    public CreditApplicationResponse submit(@PathVariable Long id) {
        return creditApplicationService.submit(id);
    }

    // Controller 只處理 HTTP contract；審核規則與 Transaction Boundary 由 Service 負責。
    // 核准已送審的授信申請。
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(
            @PathVariable Long id,
            @RequestBody ApproveCreditApplicationRequest request) {
        creditApplicationService.approve(id, request);
        return ResponseEntity.ok().build();
    }

    // 駁回已送審的授信申請。
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable Long id,
            @RequestBody RejectCreditApplicationRequest request) {
        creditApplicationService.reject(id, request);
        return ResponseEntity.ok().build();
    }
}
