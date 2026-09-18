package com.minghan.credit.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI corporateCreditOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Corporate Credit Management API")
                        .description("""
                                REST API for a corporate credit management workflow.

                                Core workflow:
                                DRAFT -> SUBMITTED -> APPROVED / REJECTED

                                Key business rules:
                                - RM creates and submits credit applications.
                                - REVIEWER approves or rejects submitted applications.
                                - Maker-Checker prevents a user from reviewing their own application.
                                - Approval creates a credit limit.
                                - Drawdown amount cannot exceed the available credit limit.
                                - JWT Bearer authentication is required for protected endpoints.
                                """)
                        .version("v1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter the JWT returned by POST /api/auth/login")));
    }
}