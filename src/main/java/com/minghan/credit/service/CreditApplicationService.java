package com.minghan.credit.service;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minghan.credit.dto.ApproveCreditApplicationRequest;
import com.minghan.credit.dto.CreateCreditApplicationRequest;
import com.minghan.credit.dto.CreditApplicationPageResponse;
import com.minghan.credit.dto.CreditApplicationResponse;
import com.minghan.credit.dto.RejectCreditApplicationRequest;
import com.minghan.credit.entity.AppUser;
import com.minghan.credit.entity.AuditAction;
import com.minghan.credit.entity.AuditEntityType;
import com.minghan.credit.entity.Company;
import com.minghan.credit.entity.CreditApplication;
import com.minghan.credit.entity.CreditApplicationStatus;
import com.minghan.credit.entity.CreditLimit;
import com.minghan.credit.entity.CreditReview;
import com.minghan.credit.entity.CreditReviewDecision;
import com.minghan.credit.exception.BusinessRuleException;
import com.minghan.credit.exception.InvalidRequestException;
import com.minghan.credit.exception.ResourceNotFoundException;
import com.minghan.credit.repository.CompanyRepository;
import com.minghan.credit.repository.AppUserRepository;
import com.minghan.credit.repository.CreditApplicationRepository;
import com.minghan.credit.repository.CreditLimitRepository;
import com.minghan.credit.repository.CreditReviewRepository;

@Service
public class CreditApplicationService {

    private final CompanyRepository companyRepository;
    private final AppUserRepository appUserRepository;
    private final CreditApplicationRepository creditApplicationRepository;
    private final CreditReviewRepository creditReviewRepository;
    private final CreditLimitRepository creditLimitRepository;
    private final AuditLogService auditLogService;

    public CreditApplicationService(
            CompanyRepository companyRepository,
            AppUserRepository appUserRepository,
            CreditApplicationRepository creditApplicationRepository,
            CreditReviewRepository creditReviewRepository,
            CreditLimitRepository creditLimitRepository,
            AuditLogService auditLogService) {
        this.companyRepository = companyRepository;
        this.appUserRepository = appUserRepository;
        this.creditApplicationRepository = creditApplicationRepository;
        this.creditReviewRepository = creditReviewRepository;
        this.creditLimitRepository = creditLimitRepository;
        this.auditLogService = auditLogService;
    }

    // 建立授信申請：
    // 1. 先確認指定 Company 存在
    // 2. 建立 CreditApplication Entity
    // 3. 初始狀態由 Entity 固定為 DRAFT
    // 4. 儲存後轉成 Response DTO 回傳
    @Transactional
    public CreditApplicationResponse create(CreateCreditApplicationRequest request) {

        BigDecimal requestedAmount = request.requestedAmount();

        if (requestedAmount == null) {
            throw new InvalidRequestException("Requested amount is required");
        }

        if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Requested amount must be greater than zero");
        }

        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        AppUser createdBy = getCurrentUser();

        CreditApplication application = new CreditApplication(
                company,
                createdBy,
                requestedAmount,
                request.purpose());

        CreditApplication savedApplication = creditApplicationRepository.save(application);
        auditLogService.record(
                AuditAction.CREATE_CREDIT_APPLICATION,
                AuditEntityType.CREDIT_APPLICATION,
                savedApplication.getId());

        return toResponse(savedApplication);
    }

    // 送出授信申請：
    // 只有 DRAFT 狀態可以轉為 SUBMITTED。
    @Transactional
    public CreditApplicationResponse submit(Long id) {
        CreditApplication application = creditApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credit application not found"));

        if (application.getStatus() != CreditApplicationStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT application can be submitted");
        }

        // 狀態轉換 Business Rule：
        // 非 DRAFT 不允許再次送審。
        application.submit();
        CreditApplication savedApplication = creditApplicationRepository.save(application);
        auditLogService.record(
                AuditAction.SUBMIT_CREDIT_APPLICATION,
                AuditEntityType.CREDIT_APPLICATION,
                savedApplication.getId());

        return toResponse(savedApplication);

    }

    // 直接使用 Repository 的 Page 查詢，只映射當頁結果，不先載入全部資料到記憶體切頁。
    @Transactional(readOnly = true)
    public CreditApplicationPageResponse findAll(
            CreditApplicationStatus status,
            Pageable pageable) {
        Page<CreditApplication> applications = status == null
                ? creditApplicationRepository.findAll(pageable)
                : creditApplicationRepository.findByStatus(status, pageable);

        return new CreditApplicationPageResponse(
                applications.getContent().stream()
                        .map(this::toResponse)
                        .toList(),
                applications.getNumber(),
                applications.getSize(),
                applications.getTotalElements(),
                applications.getTotalPages(),
                applications.isFirst(),
                applications.isLast());
    }

    // 跨 Application、Review 與 Limit 的 Business Rule 集中在 Service，避免散落在 Controller。
    // 單一 Transaction 確保狀態、Review 與 Limit 要嘛全部成功，要嘛一起 rollback。
    @Transactional
    public void approve(
            Long applicationId,
            ApproveCreditApplicationRequest request) {
        CreditApplication application = creditApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit application not found"));

        validateMakerChecker(application);

        if (application.getStatus() != CreditApplicationStatus.SUBMITTED) {
            throw new BusinessRuleException("Only SUBMITTED application can be approved");
        }

        if (creditReviewRepository.existsByApplicationId(applicationId)) {
            throw new BusinessRuleException("Credit application has already been reviewed");
        }

        if (creditLimitRepository.existsByApplicationId(applicationId)) {
            throw new BusinessRuleException("Credit limit already exists for this application");
        }

        BigDecimal approvedAmount = request.approvedAmount();

        if (approvedAmount == null) {
            throw new InvalidRequestException("Approved amount is required");
        }

        // 金額使用 compareTo 比較數值大小，避免 equals 同時比較 scale（例如 1.0 與 1.00）。
        if (approvedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Approved amount must be greater than zero");
        }

        if (approvedAmount.compareTo(application.getRequestedAmount()) > 0) {
            throw new InvalidRequestException("Approved amount cannot exceed requested amount");
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
        auditLogService.record(
                AuditAction.APPROVE_CREDIT_APPLICATION,
                AuditEntityType.CREDIT_APPLICATION,
                application.getId());
    }

    // Reject 同樣以 Transaction 維持 Application 狀態與 Review 紀錄的一致性。
    @Transactional
    public void reject(
            Long applicationId,
            RejectCreditApplicationRequest request) {
        CreditApplication application = creditApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit application not found"));

        validateMakerChecker(application);

        if (application.getStatus() != CreditApplicationStatus.SUBMITTED) {
            throw new BusinessRuleException("Only SUBMITTED application can be rejected");
        }

        if (creditReviewRepository.existsByApplicationId(applicationId)) {
            throw new BusinessRuleException("Credit application has already been reviewed");
        }

        if (request.comment() == null || request.comment().isBlank()) {
            throw new InvalidRequestException("Comment is required when rejecting an application");
        }

        CreditReview review = new CreditReview(
                application,
                CreditReviewDecision.REJECTED,
                null,
                request.comment());

        application.reject();

        creditReviewRepository.save(review);
        auditLogService.record(
                AuditAction.REJECT_CREDIT_APPLICATION,
                AuditEntityType.CREDIT_APPLICATION,
                application.getId());
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

    private AppUser getCurrentUser() {
        // JWT filter 已把 UserDetails 放入 SecurityContext，這裡再查回 domain AppUser。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated user is required");
        }

        return appUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Authenticated user not found"));
    }

    private void validateMakerChecker(CreditApplication application) {
        AppUser reviewer = getCurrentUser();
        AppUser createdBy = application.getCreatedBy();

        if (createdBy == null) {
            throw new IllegalStateException("Credit application maker is required for review");
        }

        // 比對持久化 ID，避免同一人因 Entity instance 不同而繞過職責分離規則。
        if (Objects.equals(reviewer.getId(), createdBy.getId())) {
            throw new BusinessRuleException(
                    "Maker cannot approve or reject their own credit application");
        }
    }
}
