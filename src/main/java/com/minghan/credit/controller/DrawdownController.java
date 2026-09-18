package com.minghan.credit.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.minghan.credit.config.OpenApiConfig;
import com.minghan.credit.dto.CreateDrawdownRequest;
import com.minghan.credit.dto.DrawdownResponse;
import com.minghan.credit.exception.ApiErrorResponse;
import com.minghan.credit.service.DrawdownService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/credit-limits/{creditLimitId}/drawdowns")
@Tag(name = "Drawdowns")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class DrawdownController {

    private final DrawdownService drawdownService;

    public DrawdownController(DrawdownService drawdownService) {
        this.drawdownService = drawdownService;
    }

    // 沿用 Stage 5 method security：只有 RM 可以建立 Drawdown。
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RM')")
    @Operation(summary = "Create drawdown", description = "Creates a drawdown against a credit limit and decreases the available amount. Only users with the RM role may create drawdowns.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Drawdown created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DrawdownResponse.class))),
            @ApiResponse(responseCode = "400", description = "Drawdown amount is missing or must be greater than zero", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "RM role required", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Credit limit not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Drawdown amount exceeds available credit limit", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public DrawdownResponse create(
            @PathVariable Long creditLimitId,
            @RequestBody CreateDrawdownRequest request) {
        return drawdownService.create(creditLimitId, request);
    }
}
