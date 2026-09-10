package com.minghan.credit.controller;

import com.minghan.credit.config.JwtAuthenticationFilter;
import com.minghan.credit.config.SecurityConfig;
import com.minghan.credit.dto.ApproveCreditApplicationRequest;
import com.minghan.credit.dto.CreateCreditApplicationRequest;
import com.minghan.credit.dto.CreateDrawdownRequest;
import com.minghan.credit.dto.CreditApplicationResponse;
import com.minghan.credit.dto.DrawdownResponse;
import com.minghan.credit.dto.RejectCreditApplicationRequest;
import com.minghan.credit.entity.CreditApplicationStatus;
import com.minghan.credit.service.CreditApplicationService;
import com.minghan.credit.service.CustomUserDetailsService;
import com.minghan.credit.service.DrawdownService;
import com.minghan.credit.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthController.class,
        CreditApplicationController.class,
        DrawdownController.class
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CreditWorkflowSecurityTest {

    private static final String CREATE_APPLICATION_JSON = """
            {
              "companyId": 7,
              "requestedAmount": 100.00,
              "purpose": "Working capital"
            }
            """;

    private static final String APPROVE_JSON = """
            {
              "approvedAmount": 80.00,
              "comment": "Approved"
            }
            """;

    private static final String REJECT_JSON = """
            {
              "comment": "Insufficient repayment capacity"
            }
            """;

    private static final String DRAWDOWN_JSON = """
            {
              "amount": 25.00
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreditApplicationService creditApplicationService;

    @MockitoBean
    private DrawdownService drawdownService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Test
    void givenAnonymousUser_whenCreatingCreditApplication_thenReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/credit-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(creditApplicationService);
    }

    @Test
    void givenAnonymousUser_whenLoggingIn_thenPermitRequestAndInvokeAuthentication() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("rm-user");
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken("rm-user")).thenReturn("issued-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "rm-user",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("issued-token"));

        verify(authenticationManager).authenticate(any());
    }

    @Test
    void givenRm_whenCreatingCreditApplication_thenReturnCreatedAndInvokeService() throws Exception {
        when(creditApplicationService.create(any(CreateCreditApplicationRequest.class)))
                .thenReturn(creditApplicationResponse(CreditApplicationStatus.DRAFT));

        mockMvc.perform(post("/api/credit-applications")
                        .with(user("rm-user").roles("RM"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_APPLICATION_JSON))
                .andExpect(status().isCreated());

        verify(creditApplicationService).create(any(CreateCreditApplicationRequest.class));
    }

    @Test
    void givenRm_whenSubmittingCreditApplication_thenReturnOkAndInvokeService() throws Exception {
        when(creditApplicationService.submit(42L))
                .thenReturn(creditApplicationResponse(CreditApplicationStatus.SUBMITTED));

        mockMvc.perform(post("/api/credit-applications/42/submit")
                        .with(user("rm-user").roles("RM")))
                .andExpect(status().isOk());

        verify(creditApplicationService).submit(42L);
    }

    @Test
    void givenRm_whenApprovingCreditApplication_thenReturnForbiddenWithoutInvokingService()
            throws Exception {
        mockMvc.perform(post("/api/credit-applications/42/approve")
                        .with(user("rm-user").roles("RM"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPROVE_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(creditApplicationService);
    }

    @Test
    void givenRm_whenRejectingCreditApplication_thenReturnForbiddenWithoutInvokingService()
            throws Exception {
        mockMvc.perform(post("/api/credit-applications/42/reject")
                        .with(user("rm-user").roles("RM"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REJECT_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(creditApplicationService);
    }

    @Test
    void givenRm_whenCreatingDrawdown_thenReturnCreatedAndInvokeService() throws Exception {
        when(drawdownService.create(eq(3L), any(CreateDrawdownRequest.class)))
                .thenReturn(new DrawdownResponse(
                        9L,
                        3L,
                        new BigDecimal("25.00"),
                        11L,
                        LocalDateTime.now()));

        mockMvc.perform(post("/api/credit-limits/3/drawdowns")
                        .with(user("rm-user").roles("RM"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DRAWDOWN_JSON))
                .andExpect(status().isCreated());

        verify(drawdownService).create(eq(3L), any(CreateDrawdownRequest.class));
    }

    @Test
    void givenReviewer_whenApprovingCreditApplication_thenReturnOkAndInvokeService()
            throws Exception {
        mockMvc.perform(post("/api/credit-applications/42/approve")
                        .with(user("reviewer-user").roles("REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPROVE_JSON))
                .andExpect(status().isOk());

        verify(creditApplicationService).approve(
                eq(42L),
                any(ApproveCreditApplicationRequest.class));
    }

    @Test
    void givenReviewer_whenRejectingCreditApplication_thenReturnOkAndInvokeService()
            throws Exception {
        mockMvc.perform(post("/api/credit-applications/42/reject")
                        .with(user("reviewer-user").roles("REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REJECT_JSON))
                .andExpect(status().isOk());

        verify(creditApplicationService).reject(
                eq(42L),
                any(RejectCreditApplicationRequest.class));
    }

    @Test
    void givenReviewer_whenCreatingCreditApplication_thenReturnForbiddenWithoutInvokingService()
            throws Exception {
        mockMvc.perform(post("/api/credit-applications")
                        .with(user("reviewer-user").roles("REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(creditApplicationService);
    }

    @Test
    void givenReviewer_whenSubmittingCreditApplication_thenReturnForbiddenWithoutInvokingService()
            throws Exception {
        mockMvc.perform(post("/api/credit-applications/42/submit")
                        .with(user("reviewer-user").roles("REVIEWER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(creditApplicationService);
    }

    @Test
    void givenReviewer_whenCreatingDrawdown_thenReturnForbiddenWithoutInvokingService()
            throws Exception {
        mockMvc.perform(post("/api/credit-limits/3/drawdowns")
                        .with(user("reviewer-user").roles("REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DRAWDOWN_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(drawdownService);
    }

    @Test
    void givenAdmin_whenCreatingCreditApplication_thenReturnForbiddenWithoutInvokingService()
            throws Exception {
        mockMvc.perform(post("/api/credit-applications")
                        .with(user("admin-user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(creditApplicationService);
    }

    @Test
    void givenAdmin_whenApprovingCreditApplication_thenReturnForbiddenWithoutInvokingService()
            throws Exception {
        mockMvc.perform(post("/api/credit-applications/42/approve")
                        .with(user("admin-user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPROVE_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(creditApplicationService);
    }

    @Test
    void givenAdmin_whenCreatingDrawdown_thenReturnForbiddenWithoutInvokingService()
            throws Exception {
        mockMvc.perform(post("/api/credit-limits/3/drawdowns")
                        .with(user("admin-user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DRAWDOWN_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(drawdownService);
    }

    private CreditApplicationResponse creditApplicationResponse(
            CreditApplicationStatus status) {
        return new CreditApplicationResponse(
                42L,
                7L,
                new BigDecimal("100.00"),
                "Working capital",
                status,
                LocalDateTime.now());
    }
}
