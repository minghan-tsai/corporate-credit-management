package com.minghan.credit.service;

import com.minghan.credit.dto.CreateDrawdownRequest;
import com.minghan.credit.entity.AppUser;
import com.minghan.credit.entity.CreditLimit;
import com.minghan.credit.entity.Role;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @InjectMocks
    private DrawdownService drawdownService;

    private AppUser currentUser;

    @BeforeEach
    void setUpAuthentication() {
        currentUser = new AppUser(USERNAME, "password", Role.RM);
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

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> drawdownService.create(
                        CREDIT_LIMIT_ID,
                        new CreateDrawdownRequest(BigDecimal.ONE)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(drawdownRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-0.01"})
    void createReturnsBadRequestWhenAmountIsNotPositive(BigDecimal amount) {
        CreditLimit creditLimit = creditLimitWithAvailableAmount("100.00");
        when(creditLimitRepository.findByIdForUpdate(CREDIT_LIMIT_ID))
                .thenReturn(Optional.of(creditLimit));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> drawdownService.create(
                        CREDIT_LIMIT_ID,
                        new CreateDrawdownRequest(amount)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(new BigDecimal("100.00"), creditLimit.getAvailableAmount());
        verify(drawdownRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createReturnsBadRequestWhenAmountExceedsAvailableAmount() {
        CreditLimit creditLimit = creditLimitWithAvailableAmount("100.00");
        when(creditLimitRepository.findByIdForUpdate(CREDIT_LIMIT_ID))
                .thenReturn(Optional.of(creditLimit));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> drawdownService.create(
                        CREDIT_LIMIT_ID,
                        new CreateDrawdownRequest(new BigDecimal("100.01"))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(new BigDecimal("100.00"), creditLimit.getAvailableAmount());
        verify(drawdownRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private CreditLimit creditLimitWithAvailableAmount(String amount) {
        return new CreditLimit(null, new BigDecimal(amount));
    }
}
