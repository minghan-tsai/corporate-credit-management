package com.minghan.credit.service;

import com.minghan.credit.dto.ApproveCreditApplicationRequest;
import com.minghan.credit.dto.CreateCreditApplicationRequest;
import com.minghan.credit.dto.CreditApplicationResponse;
import com.minghan.credit.dto.CreditApplicationPageResponse;
import com.minghan.credit.dto.RejectCreditApplicationRequest;
import com.minghan.credit.entity.AppUser;
import com.minghan.credit.entity.AuditAction;
import com.minghan.credit.entity.AuditEntityType;
import com.minghan.credit.entity.Company;
import com.minghan.credit.entity.CreditApplication;
import com.minghan.credit.entity.CreditApplicationStatus;
import com.minghan.credit.entity.Role;
import com.minghan.credit.exception.BusinessRuleException;
import com.minghan.credit.exception.InvalidRequestException;
import com.minghan.credit.exception.ResourceNotFoundException;
import com.minghan.credit.repository.AppUserRepository;
import com.minghan.credit.repository.CompanyRepository;
import com.minghan.credit.repository.CreditApplicationRepository;
import com.minghan.credit.repository.CreditLimitRepository;
import com.minghan.credit.repository.CreditReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditApplicationServiceTest {

    private static final String USERNAME = "reviewer";
    private static final Long APPLICATION_ID = 42L;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CreditApplicationRepository creditApplicationRepository;

    @Mock
    private CreditReviewRepository creditReviewRepository;

    @Mock
    private CreditLimitRepository creditLimitRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CreditApplicationService creditApplicationService;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitThrowsResourceNotFoundWhenApplicationDoesNotExist() {
        when(creditApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> creditApplicationService.submit(APPLICATION_ID));

        assertEquals("Credit application not found", exception.getMessage());
        verify(creditApplicationRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(auditLogService, never()).record(any(), any(), any());
    }

    @Test
    void submitThrowsBusinessRuleConflictWhenApplicationIsNotDraft() {
        CreditApplication application = new CreditApplication(
                null,
                null,
                BigDecimal.ONE,
                "Working capital");
        application.submit();
        when(creditApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> creditApplicationService.submit(APPLICATION_ID));

        assertEquals("Only DRAFT application can be submitted", exception.getMessage());
        verify(creditApplicationRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(auditLogService, never()).record(any(), any(), any());
    }

    @Test
    void createRecordsAuditAfterApplicationIsSaved() {
        Company company = company(7L);
        AppUser actor = appUser(USERNAME, 2L, Role.RM);
        authenticate(actor);
        when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
        when(creditApplicationRepository.save(any(CreditApplication.class)))
                .thenAnswer(invocation -> {
                    CreditApplication saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", APPLICATION_ID);
                    return saved;
                });

        CreditApplicationResponse response = creditApplicationService.create(
                new CreateCreditApplicationRequest(
                        7L,
                        new BigDecimal("100.00"),
                        "Working capital"));

        assertEquals(APPLICATION_ID, response.id());
        verify(auditLogService).record(
                AuditAction.CREATE_CREDIT_APPLICATION,
                AuditEntityType.CREDIT_APPLICATION,
                APPLICATION_ID);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-0.01"})
    void givenRequestedAmountIsNullZeroOrNegative_whenCreate_thenRejectWithoutWrites(
            BigDecimal requestedAmount) {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> creditApplicationService.create(
                        new CreateCreditApplicationRequest(
                                7L,
                                requestedAmount,
                                "Working capital")));

        assertEquals(
                requestedAmount == null
                        ? "Requested amount is required"
                        : "Requested amount must be greater than zero",
                exception.getMessage());
        verify(companyRepository, never()).findById(any());
        verify(creditApplicationRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any());
    }

    @Test
    void submitRecordsAuditAfterStateChangeIsSaved() {
        CreditApplication application = application(
                appUser("maker", 1L, Role.RM),
                CreditApplicationStatus.DRAFT);
        when(creditApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));
        when(creditApplicationRepository.save(application)).thenReturn(application);

        CreditApplicationResponse response = creditApplicationService.submit(APPLICATION_ID);

        assertEquals(CreditApplicationStatus.SUBMITTED, response.status());
        verify(auditLogService).record(
                AuditAction.SUBMIT_CREDIT_APPLICATION,
                AuditEntityType.CREDIT_APPLICATION,
                APPLICATION_ID);
    }

    @Test
    void approveRecordsAuditAfterReviewAndLimitAreSaved() {
        AppUser reviewer = appUser(USERNAME, 2L, Role.REVIEWER);
        authenticate(reviewer);
        CreditApplication application = application(
                appUser("maker", 1L, Role.RM),
                CreditApplicationStatus.SUBMITTED);
        when(creditApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));

        creditApplicationService.approve(
                APPLICATION_ID,
                new ApproveCreditApplicationRequest(new BigDecimal("80.00"), "Approved"));

        assertEquals(CreditApplicationStatus.APPROVED, application.getStatus());
        verify(creditReviewRepository).save(any());
        verify(creditLimitRepository).save(any());
        verify(auditLogService).record(
                AuditAction.APPROVE_CREDIT_APPLICATION,
                AuditEntityType.CREDIT_APPLICATION,
                APPLICATION_ID);
    }

    @Test
    void rejectRecordsAuditAfterReviewIsSaved() {
        AppUser reviewer = appUser(USERNAME, 2L, Role.REVIEWER);
        authenticate(reviewer);
        CreditApplication application = application(
                appUser("maker", 1L, Role.RM),
                CreditApplicationStatus.SUBMITTED);
        when(creditApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));

        creditApplicationService.reject(
                APPLICATION_ID,
                new RejectCreditApplicationRequest("Insufficient repayment capacity"));

        assertEquals(CreditApplicationStatus.REJECTED, application.getStatus());
        verify(creditReviewRepository).save(any());
        verify(auditLogService).record(
                AuditAction.REJECT_CREDIT_APPLICATION,
                AuditEntityType.CREDIT_APPLICATION,
                APPLICATION_ID);
    }

    @Test
    void givenMakerIsReviewer_whenApprove_thenRejectWithoutCreatingReviewOrLimit() {
        AppUser makerAndReviewer = appUser(USERNAME, 2L, Role.REVIEWER);
        authenticate(makerAndReviewer);
        CreditApplication application = application(
                makerAndReviewer,
                CreditApplicationStatus.SUBMITTED);
        when(creditApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> creditApplicationService.approve(
                        APPLICATION_ID,
                        new ApproveCreditApplicationRequest(
                                new BigDecimal("80.00"),
                                "Approved")));

        assertEquals(
                "Maker cannot approve or reject their own credit application",
                exception.getMessage());
        assertEquals(CreditApplicationStatus.SUBMITTED, application.getStatus());
        verify(creditReviewRepository, never()).save(any());
        verify(creditLimitRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any());
    }

    @Test
    void givenMakerIsReviewer_whenReject_thenRejectWithoutCreatingReview() {
        AppUser makerAndReviewer = appUser(USERNAME, 2L, Role.REVIEWER);
        authenticate(makerAndReviewer);
        CreditApplication application = application(
                makerAndReviewer,
                CreditApplicationStatus.SUBMITTED);
        when(creditApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> creditApplicationService.reject(
                        APPLICATION_ID,
                        new RejectCreditApplicationRequest("Insufficient repayment capacity")));

        assertEquals(
                "Maker cannot approve or reject their own credit application",
                exception.getMessage());
        assertEquals(CreditApplicationStatus.SUBMITTED, application.getStatus());
        verify(creditReviewRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any());
    }

    @Test
    void givenApplicationIsNotSubmitted_whenApprove_thenRejectWithoutCreatingReviewOrLimit() {
        AppUser reviewer = appUser(USERNAME, 2L, Role.REVIEWER);
        authenticate(reviewer);
        CreditApplication application = application(
                appUser("maker", 1L, Role.RM),
                CreditApplicationStatus.DRAFT);
        when(creditApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> creditApplicationService.approve(
                        APPLICATION_ID,
                        new ApproveCreditApplicationRequest(
                                new BigDecimal("80.00"),
                                "Approved")));

        assertEquals("Only SUBMITTED application can be approved", exception.getMessage());
        assertEquals(CreditApplicationStatus.DRAFT, application.getStatus());
        verify(creditReviewRepository, never()).save(any());
        verify(creditLimitRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-0.01"})
    void givenApprovedAmountIsNullZeroOrNegative_whenApprove_thenRejectWithoutWrites(
            BigDecimal approvedAmount) {
        AppUser reviewer = appUser(USERNAME, 2L, Role.REVIEWER);
        authenticate(reviewer);
        CreditApplication application = application(
                appUser("maker", 1L, Role.RM),
                CreditApplicationStatus.SUBMITTED);
        when(creditApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> creditApplicationService.approve(
                        APPLICATION_ID,
                        new ApproveCreditApplicationRequest(
                                approvedAmount,
                                "Approved")));

        assertEquals(
                approvedAmount == null
                        ? "Approved amount is required"
                        : "Approved amount must be greater than zero",
                exception.getMessage());
        assertEquals(CreditApplicationStatus.SUBMITTED, application.getStatus());
        verify(creditReviewRepository, never()).save(any());
        verify(creditLimitRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any());
    }

    @Test
    void givenApprovedAmountExceedsRequestedAmount_whenApprove_thenRejectWithoutWrites() {
        AppUser reviewer = appUser(USERNAME, 2L, Role.REVIEWER);
        authenticate(reviewer);
        CreditApplication application = application(
                appUser("maker", 1L, Role.RM),
                CreditApplicationStatus.SUBMITTED);
        when(creditApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> creditApplicationService.approve(
                        APPLICATION_ID,
                        new ApproveCreditApplicationRequest(
                                new BigDecimal("100.01"),
                                "Amount exceeds request")));

        assertEquals("Approved amount cannot exceed requested amount", exception.getMessage());
        assertEquals(CreditApplicationStatus.SUBMITTED, application.getStatus());
        verify(creditReviewRepository, never()).save(any());
        verify(creditLimitRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any());
    }

    @Test
    void givenReviewAlreadyExists_whenApprove_thenRejectDuplicateReviewWithoutCreatingLimit() {
        AppUser reviewer = appUser(USERNAME, 2L, Role.REVIEWER);
        authenticate(reviewer);
        CreditApplication application = application(
                appUser("maker", 1L, Role.RM),
                CreditApplicationStatus.SUBMITTED);
        when(creditApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));
        when(creditReviewRepository.existsByApplicationId(APPLICATION_ID)).thenReturn(true);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> creditApplicationService.approve(
                        APPLICATION_ID,
                        new ApproveCreditApplicationRequest(
                                new BigDecimal("80.00"),
                                "Approved")));

        assertEquals("Credit application has already been reviewed", exception.getMessage());
        assertEquals(CreditApplicationStatus.SUBMITTED, application.getStatus());
        verify(creditReviewRepository, never()).save(any());
        verify(creditLimitRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any());
    }

    @Test
    void givenCreditLimitAlreadyExists_whenApprove_thenRejectWithoutCreatingAnotherLimit() {
        AppUser reviewer = appUser(USERNAME, 2L, Role.REVIEWER);
        authenticate(reviewer);
        CreditApplication application = application(
                appUser("maker", 1L, Role.RM),
                CreditApplicationStatus.SUBMITTED);
        when(creditApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));
        when(creditLimitRepository.existsByApplicationId(APPLICATION_ID)).thenReturn(true);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> creditApplicationService.approve(
                        APPLICATION_ID,
                        new ApproveCreditApplicationRequest(
                                new BigDecimal("80.00"),
                                "Approved")));

        assertEquals("Credit limit already exists for this application", exception.getMessage());
        assertEquals(CreditApplicationStatus.SUBMITTED, application.getStatus());
        verify(creditReviewRepository, never()).save(any());
        verify(creditLimitRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any());
    }

    @Test
    void givenApplicationIsNotSubmitted_whenReject_thenRejectWithoutCreatingReview() {
        AppUser reviewer = appUser(USERNAME, 2L, Role.REVIEWER);
        authenticate(reviewer);
        CreditApplication application = application(
                appUser("maker", 1L, Role.RM),
                CreditApplicationStatus.DRAFT);
        when(creditApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> creditApplicationService.reject(
                        APPLICATION_ID,
                        new RejectCreditApplicationRequest("Insufficient repayment capacity")));

        assertEquals("Only SUBMITTED application can be rejected", exception.getMessage());
        assertEquals(CreditApplicationStatus.DRAFT, application.getStatus());
        verify(creditReviewRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    void givenRejectReasonIsNullOrBlank_whenReject_thenRejectWithoutCreatingReview(String reason) {
        AppUser reviewer = appUser(USERNAME, 2L, Role.REVIEWER);
        authenticate(reviewer);
        CreditApplication application = application(
                appUser("maker", 1L, Role.RM),
                CreditApplicationStatus.SUBMITTED);
        when(creditApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> creditApplicationService.reject(
                        APPLICATION_ID,
                        new RejectCreditApplicationRequest(reason)));

        assertEquals(
                "Comment is required when rejecting an application",
                exception.getMessage());
        assertEquals(CreditApplicationStatus.SUBMITTED, application.getStatus());
        verify(creditReviewRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any());
    }

    @Test
    void givenReviewAlreadyExists_whenReject_thenRejectDuplicateReviewWithoutCreatingAnotherReview() {
        AppUser reviewer = appUser(USERNAME, 2L, Role.REVIEWER);
        authenticate(reviewer);
        CreditApplication application = application(
                appUser("maker", 1L, Role.RM),
                CreditApplicationStatus.SUBMITTED);
        when(creditApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));
        when(creditReviewRepository.existsByApplicationId(APPLICATION_ID)).thenReturn(true);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> creditApplicationService.reject(
                        APPLICATION_ID,
                        new RejectCreditApplicationRequest("Insufficient repayment capacity")));

        assertEquals("Credit application has already been reviewed", exception.getMessage());
        assertEquals(CreditApplicationStatus.SUBMITTED, application.getStatus());
        verify(creditReviewRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any());
    }

    @Test
    void findAllWithoutStatusUsesPagedRepositoryAndMapsResponse() {
        Pageable pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "id"));
        CreditApplication application = application(
                appUser("maker", 1L, Role.RM),
                CreditApplicationStatus.DRAFT);
        when(creditApplicationRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(application), pageable, 5));

        CreditApplicationPageResponse response = creditApplicationService.findAll(null, pageable);

        assertEquals(1, response.content().size());
        assertEquals(APPLICATION_ID, response.content().getFirst().id());
        assertEquals(1, response.page());
        assertEquals(2, response.size());
        assertEquals(5, response.totalElements());
        assertEquals(3, response.totalPages());
        verify(creditApplicationRepository).findAll(pageable);
        verify(creditApplicationRepository, never()).findByStatus(any(), any());
    }

    @Test
    void findAllWithStatusUsesFilteredPagedRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        CreditApplication application = application(
                appUser("maker", 1L, Role.RM),
                CreditApplicationStatus.SUBMITTED);
        when(creditApplicationRepository.findByStatus(
                CreditApplicationStatus.SUBMITTED,
                pageable))
                .thenReturn(new PageImpl<>(List.of(application), pageable, 1));

        CreditApplicationPageResponse response = creditApplicationService.findAll(
                CreditApplicationStatus.SUBMITTED,
                pageable);

        assertEquals(1, response.content().size());
        assertEquals(CreditApplicationStatus.SUBMITTED, response.content().getFirst().status());
        assertEquals(1, response.totalElements());
        verify(creditApplicationRepository).findByStatus(
                CreditApplicationStatus.SUBMITTED,
                pageable);
        verify(creditApplicationRepository, never()).findAll(pageable);
    }

    private void authenticate(AppUser actor) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USERNAME, null, List.of()));
        when(appUserRepository.findByUsername(USERNAME)).thenReturn(Optional.of(actor));
    }

    private Company company(Long id) {
        Company company = new Company("Example Company", "12345678");
        ReflectionTestUtils.setField(company, "id", id);
        return company;
    }

    private AppUser appUser(String username, Long id, Role role) {
        AppUser appUser = new AppUser(username, "password", role);
        ReflectionTestUtils.setField(appUser, "id", id);
        return appUser;
    }

    private CreditApplication application(
            AppUser maker,
            CreditApplicationStatus targetStatus) {
        CreditApplication application = new CreditApplication(
                company(7L),
                maker,
                new BigDecimal("100.00"),
                "Working capital");
        ReflectionTestUtils.setField(application, "id", APPLICATION_ID);

        if (targetStatus == CreditApplicationStatus.SUBMITTED) {
            application.submit();
        }

        return application;
    }
}
