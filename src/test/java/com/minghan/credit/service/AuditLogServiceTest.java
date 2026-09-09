package com.minghan.credit.service;

import com.minghan.credit.dto.ApproveCreditApplicationRequest;
import com.minghan.credit.dto.CreateCreditApplicationRequest;
import com.minghan.credit.dto.CreateDrawdownRequest;
import com.minghan.credit.dto.RejectCreditApplicationRequest;
import com.minghan.credit.entity.AuditAction;
import com.minghan.credit.entity.AuditEntityType;
import com.minghan.credit.entity.AuditLog;
import com.minghan.credit.repository.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordPersistsAuthenticatedUsernameAndTypedAuditData() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("stage7_rm", null, List.of()));

        auditLogService.record(
                AuditAction.CREATE_DRAWDOWN,
                AuditEntityType.DRAWDOWN,
                9L);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals("stage7_rm", saved.getActorUsername());
        assertEquals(AuditAction.CREATE_DRAWDOWN, saved.getAction());
        assertEquals(AuditEntityType.DRAWDOWN, saved.getEntityType());
        assertEquals(9L, saved.getEntityId());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void recordRejectsMissingAuthenticationWithoutWritingAudit() {
        assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> auditLogService.record(
                        AuditAction.CREATE_DRAWDOWN,
                        AuditEntityType.DRAWDOWN,
                        9L));

        verify(auditLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recordRequiresAnExistingBusinessTransaction() throws NoSuchMethodException {
        Transactional transactional = AuditLogService.class
                .getMethod(
                        "record",
                        AuditAction.class,
                        AuditEntityType.class,
                        Long.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertEquals(Propagation.MANDATORY, transactional.propagation());
    }

    @Test
    void allAuditedBusinessActionsHaveTransactionBoundaries() throws NoSuchMethodException {
        assertTransactional(
                CreditApplicationService.class,
                "create",
                CreateCreditApplicationRequest.class);
        assertTransactional(CreditApplicationService.class, "submit", Long.class);
        assertTransactional(
                CreditApplicationService.class,
                "approve",
                Long.class,
                ApproveCreditApplicationRequest.class);
        assertTransactional(
                CreditApplicationService.class,
                "reject",
                Long.class,
                RejectCreditApplicationRequest.class);
        assertTransactional(
                DrawdownService.class,
                "create",
                Long.class,
                CreateDrawdownRequest.class);
    }

    private void assertTransactional(
            Class<?> serviceType,
            String methodName,
            Class<?>... parameterTypes) throws NoSuchMethodException {
        Transactional transactional = serviceType
                .getMethod(methodName, parameterTypes)
                .getAnnotation(Transactional.class);

        assertNotNull(
                transactional,
                () -> serviceType.getSimpleName() + "." + methodName
                        + " must have a transaction boundary");
    }
}
