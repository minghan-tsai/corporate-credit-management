package com.minghan.credit.exception;

import com.minghan.credit.controller.CompanyController;
import com.minghan.credit.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private CompanyService companyService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        companyService = mock(CompanyService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CompanyController(companyService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void resourceNotFoundUsesConsistent404Response() throws Exception {
        when(companyService.getById(42L))
                .thenThrow(new ResourceNotFoundException("Company not found"));

        mockMvc.perform(get("/api/companies/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Company not found"))
                .andExpect(jsonPath("$.path").value("/api/companies/42"))
                .andExpect(jsonPath("$.validationErrors").doesNotExist());
    }

    @Test
    void invalidRequestUsesConsistent400Response() throws Exception {
        when(companyService.getById(42L))
                .thenThrow(new InvalidRequestException("Invalid request"));

        mockMvc.perform(get("/api/companies/42"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid request"));
    }

    @Test
    void businessRuleUsesConsistent409Response() throws Exception {
        when(companyService.getById(42L))
                .thenThrow(new BusinessRuleException("Business rule conflict"));

        mockMvc.perform(get("/api/companies/42"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Business rule conflict"));
    }

    @Test
    void beanValidationReturnsFieldDetails() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "taxId": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/companies"))
                .andExpect(jsonPath("$.validationErrors.length()").value(2))
                .andExpect(jsonPath("$.validationErrors[0].field").value("name"))
                .andExpect(jsonPath("$.validationErrors[1].field").value("taxId"));
    }

    @Test
    void malformedJsonUsesConsistent400Response() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.validationErrors").doesNotExist());
    }

    @Test
    void invalidPathVariableUsesConsistent400Response() throws Exception {
        mockMvc.perform(get("/api/companies/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter: id"));
    }
}
