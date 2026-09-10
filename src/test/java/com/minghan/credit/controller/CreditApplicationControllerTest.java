package com.minghan.credit.controller;

import com.minghan.credit.dto.CreditApplicationPageResponse;
import com.minghan.credit.exception.GlobalExceptionHandler;
import com.minghan.credit.service.CreditApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CreditApplicationControllerTest {

    private CreditApplicationService creditApplicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        creditApplicationService = mock(CreditApplicationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CreditApplicationController(creditApplicationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new SortHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void listBindsPageSizeAndStandardSortParameters() throws Exception {
        when(creditApplicationService.findAll(isNull(), any(Pageable.class)))
                .thenReturn(emptyPage(2, 5));

        mockMvc.perform(get("/api/credit-applications")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "createdAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(creditApplicationService).findAll(isNull(), captor.capture());
        Pageable pageable = captor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        assertEquals(
                Sort.Direction.ASC,
                pageable.getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void listRejectsInvalidStatusWith400() throws Exception {
        mockMvc.perform(get("/api/credit-applications")
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Invalid value for parameter: status"));

        verify(creditApplicationService, never()).findAll(any(), any());
    }

    @Test
    void listRejectsSizeAboveMaximumWith400() throws Exception {
        mockMvc.perform(get("/api/credit-applications")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Size must not exceed 100"));

        verify(creditApplicationService, never()).findAll(any(), any());
    }

    @Test
    void listRejectsNegativePageWith400() throws Exception {
        mockMvc.perform(get("/api/credit-applications")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Page must be zero or greater"));

        verify(creditApplicationService, never()).findAll(any(), any());
    }

    private CreditApplicationPageResponse emptyPage(int page, int size) {
        return new CreditApplicationPageResponse(
                List.of(),
                page,
                size,
                0,
                0,
                true,
                true);
    }
}
