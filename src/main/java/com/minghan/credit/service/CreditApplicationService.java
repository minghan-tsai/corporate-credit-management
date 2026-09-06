package com.minghan.credit.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minghan.credit.dto.ApproveCreditApplicationRequest;
import com.minghan.credit.dto.CreateCreditApplicationRequest;
import com.minghan.credit.dto.CreditApplicationResponse;
import com.minghan.credit.dto.RejectCreditApplicationRequest;
import com.minghan.credit.entity.Company;
import com.minghan.credit.entity.CreditApplication;
import com.minghan.credit.entity.CreditApplicationStatus;
import com.minghan.credit.entity.CreditLimit;
import com.minghan.credit.entity.CreditReview;
import com.minghan.credit.entity.CreditReviewDecision;
import com.minghan.credit.repository.CompanyRepository;
import com.minghan.credit.repository.CreditApplicationRepository;
import com.minghan.credit.repository.CreditLimitRepository;
import com.minghan.credit.repository.CreditReviewRepository;

@Service
public class CreditApplicationService {

    private final CompanyRepository companyRepository;
    private final CreditApplicationRepository creditApplicationRepository;
    private final CreditReviewRepository creditReviewRepository;
    private final CreditLimitRepository creditLimitRepository;

    public CreditApplicationService(
            CompanyRepository companyRepository,
            CreditApplicationRepository creditApplicationRepository,
            CreditReviewRepository creditReviewRepository,
            CreditLimitRepository creditLimitRepository) {
        this.companyRepository = companyRepository;
        this.creditApplicationRepository = creditApplicationRepository;
        this.creditReviewRepository = creditReviewRepository;
        this.creditLimitRepository = creditLimitRepository;
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

    // 跨 Application、Review 與 Limit 的 Business Rule 集中在 Service，避免散落在 Controller。
    // 單一 Transaction 確保狀態、Review 與 Limit 要嘛全部成功，要嘛一起 rollback。
    @Transactional
    public void approve(
            Long applicationId,
            ApproveCreditApplicationRequest request) {
        CreditApplication application = creditApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Credit application not found"));

        if (application.getStatus() != CreditApplicationStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED application can be approved");
        }

        if (creditReviewRepository.existsByApplicationId(applicationId)) {
            throw new IllegalStateException("Credit application has already been reviewed");
        }

        if (creditLimitRepository.existsByApplicationId(applicationId)) {
            throw new IllegalStateException("Credit limit already exists for this application");
        }

        BigDecimal approvedAmount = request.approvedAmount();

        if (approvedAmount == null) {
            throw new IllegalArgumentException("Approved amount is required");
        }

        // 金額使用 compareTo 比較數值大小，避免 equals 同時比較 scale（例如 1.0 與 1.00）。
        if (approvedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Approved amount must be greater than zero");
        }

        if (approvedAmount.compareTo(application.getRequestedAmount()) > 0) {
            throw new IllegalArgumentException("Approved amount cannot exceed requested amount");
        }

        CreditReview review = new CreditReview(
                application,
                CreditReviewDecision.APPROVED,
                approvedAmount,
                request.comment());

        // application 由 Repository 在本 Transaction 載入，Dirty Checking 會持久化狀態變更。
        application.approve();

        CreditLimit creditLimit = new CreditLimit(application, approvedAmount);

        creditReviewRepository.save(review);
        creditLimitRepository.save(creditLimit);
    }

    // Reject 同樣以 Transaction 維持 Application 狀態與 Review 紀錄的一致性。
    @Transactional
    public void reject(
            Long applicationId,
            RejectCreditApplicationRequest request) {
        CreditApplication application = creditApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Credit application not found"));

        if (application.getStatus() != CreditApplicationStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED application can be rejected");
        }

        if (creditReviewRepository.existsByApplicationId(applicationId)) {
            throw new IllegalStateException("Credit application has already been reviewed");
        }

        if (request.comment() == null || request.comment().isBlank()) {
            throw new IllegalArgumentException("Comment is required when rejecting an application");
        }

        CreditReview review = new CreditReview(
                application,
                CreditReviewDecision.REJECTED,
                null,
                request.comment());

        application.reject();

        creditReviewRepository.save(review);
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
