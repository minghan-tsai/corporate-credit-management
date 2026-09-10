package com.minghan.credit.service;

import com.minghan.credit.dto.CreateDrawdownRequest;
import com.minghan.credit.dto.DrawdownResponse;
import com.minghan.credit.entity.AppUser;
import com.minghan.credit.entity.AuditAction;
import com.minghan.credit.entity.AuditEntityType;
import com.minghan.credit.entity.CreditLimit;
import com.minghan.credit.entity.Drawdown;
import com.minghan.credit.entity.Role;
import com.minghan.credit.exception.BusinessRuleException;
import com.minghan.credit.exception.InvalidRequestException;
import com.minghan.credit.exception.ResourceNotFoundException;
import com.minghan.credit.repository.AppUserRepository;
import com.minghan.credit.repository.CreditLimitRepository;
import com.minghan.credit.repository.DrawdownRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DrawdownServiceTest {

    private static final String USERNAME = "stage6_rm";
    private static final Long CREDIT_LIMIT_ID = 3L;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CreditLimitRepository creditLimitRepository;

    @Mock
    private DrawdownRepository drawdownRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private DrawdownService drawdownService;

    private AppUser currentUser;

    @BeforeEach
    void setUpAuthentication() {
        currentUser = new AppUser(USERNAME, "password", Role.RM);
        ReflectionTestUtils.setField(currentUser, "id", 11L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USERNAME, null, List.of()));
        when(appUserRepository.findByUsername(USERNAME)).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createReturnsNotFoundWhenCreditLimitDoesNotExist() {
        when(creditLimitRepository.findByIdForUpdate(CREDIT_LIMIT_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> drawdownService.create(
                        CREDIT_LIMIT_ID,
                        new CreateDrawdownRequest(BigDecimal.ONE)));

        assertEquals("Credit limit not found", exception.getMessage());
        verify(drawdownRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(auditLogService, never()).record(any(), any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-0.01"})
    void createReturnsBadRequestWhenAmountIsNotPositive(BigDecimal amount) {
        CreditLimit creditLimit = creditLimitWithAvailableAmount("100.00");
        when(creditLimitRepository.findByIdForUpdate(CREDIT_LIMIT_ID))
                .thenReturn(Optional.of(creditLimit));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> drawdownService.create(
                        CREDIT_LIMIT_ID,
                        new CreateDrawdownRequest(amount)));

        assertEquals(
                amount == null
                        ? "Drawdown amount is required"
                        : "Drawdown amount must be greater than zero",
                exception.getMessage());
        assertEquals(new BigDecimal("100.00"), creditLimit.getAvailableAmount());
        verify(drawdownRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(auditLogService, never()).record(any(), any(), any());
    }

    @Test
    void createReturnsConflictWhenAmountExceedsAvailableAmount() {
        CreditLimit creditLimit = creditLimitWithAvailableAmount("100.00");
        when(creditLimitRepository.findByIdForUpdate(CREDIT_LIMIT_ID))
                .thenReturn(Optional.of(creditLimit));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> drawdownService.create(
                        CREDIT_LIMIT_ID,
                        new CreateDrawdownRequest(new BigDecimal("100.01"))));

        assertEquals("Drawdown amount cannot exceed available amount", exception.getMessage());
        assertEquals(new BigDecimal("100.00"), creditLimit.getAvailableAmount());
        verify(drawdownRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(auditLogService, never()).record(any(), any(), any());
    }

    @Test
    void createRecordsAuditAfterDrawdownAndBalanceChange() {
        CreditLimit creditLimit = creditLimitWithAvailableAmount("100.00");
        ReflectionTestUtils.setField(creditLimit, "id", CREDIT_LIMIT_ID);
        when(creditLimitRepository.findByIdForUpdate(CREDIT_LIMIT_ID))
                .thenReturn(Optional.of(creditLimit));
        when(drawdownRepository.save(any(Drawdown.class)))
                .thenAnswer(invocation -> {
                    Drawdown saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 9L);
                    return saved;
                });

        DrawdownResponse response = drawdownService.create(
                CREDIT_LIMIT_ID,
                new CreateDrawdownRequest(new BigDecimal("25.00")));

        assertEquals(9L, response.id());
        assertEquals(new BigDecimal("75.00"), creditLimit.getAvailableAmount());
        verify(auditLogService).record(
                AuditAction.CREATE_DRAWDOWN,
                AuditEntityType.DRAWDOWN,
                9L);
    }

    @Test
    void givenAmountEqualsAvailableAmount_whenCreate_thenPersistDrawdownAndExhaustLimit() {
        BigDecimal availableAmount = new BigDecimal("100.00");
        CreditLimit creditLimit = creditLimitWithAvailableAmount("100.00");
        ReflectionTestUtils.setField(creditLimit, "id", CREDIT_LIMIT_ID);
        when(creditLimitRepository.findByIdForUpdate(CREDIT_LIMIT_ID))
                .thenReturn(Optional.of(creditLimit));
        when(drawdownRepository.save(any(Drawdown.class)))
                .thenAnswer(invocation -> {
                    Drawdown saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 10L);
                    return saved;
                });

        DrawdownResponse response = drawdownService.create(
                CREDIT_LIMIT_ID,
                new CreateDrawdownRequest(availableAmount));

        assertEquals(10L, response.id());
        assertEquals(new BigDecimal("0.00"), creditLimit.getAvailableAmount());

        ArgumentCaptor<Drawdown> captor = ArgumentCaptor.forClass(Drawdown.class);
        verify(drawdownRepository).save(captor.capture());
        Drawdown savedDrawdown = captor.getValue();
        assertSame(creditLimit, savedDrawdown.getCreditLimit());
        assertEquals(availableAmount, savedDrawdown.getAmount());
        assertSame(currentUser, savedDrawdown.getCreatedBy());
        verify(auditLogService).record(
                AuditAction.CREATE_DRAWDOWN,
                AuditEntityType.DRAWDOWN,
                10L);
    }

    private CreditLimit creditLimitWithAvailableAmount(String amount) {
        return new CreditLimit(null, new BigDecimal(amount));
    }
}
