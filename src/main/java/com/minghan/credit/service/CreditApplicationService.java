package com.minghan.credit.service;

import org.springframework.stereotype.Service;

import com.minghan.credit.dto.CreateCreditApplicationRequest;
import com.minghan.credit.dto.CreditApplicationResponse;
import com.minghan.credit.entity.Company;
import com.minghan.credit.entity.CreditApplication;
import com.minghan.credit.entity.CreditApplicationStatus;
import com.minghan.credit.repository.CompanyRepository;
import com.minghan.credit.repository.CreditApplicationRepository;

@Service
public class CreditApplicationService {

    private final CompanyRepository companyRepository;
    private final CreditApplicationRepository creditApplicationRepository;

    public CreditApplicationService(
            CompanyRepository companyRepository,
            CreditApplicationRepository creditApplicationRepository) {
        this.companyRepository = companyRepository;
        this.creditApplicationRepository = creditApplicationRepository;
    }

    // 建立授信申請：
    // 1. 先確認指定 Company 存在
    // 2. 建立 CreditApplication Entity
    // 3. 初始狀態由 Entity 固定為 DRAFT
    // 4. 儲存後轉成 Response DTO 回傳
    public CreditApplicationResponse create(CreateCreditApplicationRequest request) {

        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        CreditApplication application = new CreditApplication(
                company,
                request.requestedAmount(),
                request.purpose());

        CreditApplication savedApplication = creditApplicationRepository.save(application);

        return toResponse(savedApplication);
    }

    // 送出授信申請：
    // 只有 DRAFT 狀態可以轉為 SUBMITTED。
    public CreditApplicationResponse submit(Long id) {
        CreditApplication application = creditApplicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credit application not found"));

        if (application.getStatus() != CreditApplicationStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT application can be submitted");
        }

        // 狀態轉換 Business Rule：
        // 非 DRAFT 不允許再次送審。
        application.submit();
        CreditApplication savedApplication = creditApplicationRepository.save(application);

        return toResponse(savedApplication);

    }

    // Entity 不直接作為 API Response 回傳。
    // 僅挑選 API 需要的欄位，避免 Hibernate LAZY Proxy
    // 在 JSON 序列化時造成 HttpMessageConversionException。
    private CreditApplicationResponse toResponse(CreditApplication application) {
        return new CreditApplicationResponse(
                application.getId(),
                application.getCompany().getId(),
                application.getRequestedAmount(),
                application.getPurpose(),
                application.getStatus(),
                application.getCreatedAt());
    }
}