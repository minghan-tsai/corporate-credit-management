package com.minghan.credit.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.minghan.credit.config.OpenApiConfig;
import com.minghan.credit.dto.ApproveCreditApplicationRequest;
import com.minghan.credit.dto.CreateCreditApplicationRequest;
import com.minghan.credit.dto.CreditApplicationPageResponse;
import com.minghan.credit.dto.CreditApplicationResponse;
import com.minghan.credit.dto.RejectCreditApplicationRequest;
import com.minghan.credit.entity.CreditApplicationStatus;
import com.minghan.credit.exception.ApiErrorResponse;
import com.minghan.credit.exception.InvalidRequestException;
import com.minghan.credit.service.CreditApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/credit-applications")
@Tag(name = "Credit Applications")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CreditApplicationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CreditApplicationService creditApplicationService;

    public CreditApplicationController(
            CreditApplicationService creditApplicationService) {
        this.creditApplicationService = creditApplicationService;
    }

    // 建立授信申請，成功後回傳 HTTP 201 Created。
    // RBAC 在方法入口阻擋非 RM，maker 則由 Service 從 SecurityContext 決定。
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RM')")
    @Operation(summary = "Create credit application", description = "Creates a new credit application in DRAFT status. Only users with the RM role may create applications.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Credit application created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreditApplicationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid requested amount or malformed request", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "RM role required", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Company not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public CreditApplicationResponse create(
            @RequestBody CreateCreditApplicationRequest request) {
        return creditApplicationService.create(request);
    }

    // 將指定授信申請由 DRAFT 送審為 SUBMITTED。
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('RM')")
    @Operation(summary = "Submit credit application", description = "Changes a credit application from DRAFT to SUBMITTED. Only users with the RM role may submit applications.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credit application submitted", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreditApplicationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "RM role required", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Credit application not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Application is not in DRAFT status", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public CreditApplicationResponse submit(@PathVariable Long id) {
        return creditApplicationService.submit(id);
    }

    @GetMapping
    @Operation(summary = "List credit applications", description = "Returns credit applications with optional status filtering, pagination, and sorting.", parameters = @Parameter(name = "sort", in = ParameterIn.QUERY, description = "Sort format: property,direction", example = "createdAt,desc", schema = @Schema(type = "string", defaultValue = "id,desc")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credit applications returned successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreditApplicationPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or query parameter", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public CreditApplicationPageResponse findAll(
            @RequestParam(required = false) CreditApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true) @SortDefault(sort = "id", direction = Sort.Direction.DESC) Sort sort) {

        validatePagination(page, size);

        return creditApplicationService.findAll(
                status,
                PageRequest.of(page, size, sort));
    }

    // Controller 只處理 HTTP contract；審核規則與 Transaction Boundary 由 Service 負責。
    // 核准已送審的授信申請。
    // REVIEWER 權限只代表可以審核，仍須通過 Service 層 Maker-Checker。
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('REVIEWER')")
    @Operation(summary = "Approve credit application", description = "Approves a SUBMITTED credit application. Maker-Checker prevents the application creator from approving their own application.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credit application approved"),
            @ApiResponse(responseCode = "400", description = "Invalid approved amount", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "REVIEWER role required", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Credit application not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Business rule conflict, including invalid status, duplicate review, existing credit limit, or Maker-Checker violation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> approve(
            @PathVariable Long id,
            @RequestBody ApproveCreditApplicationRequest request) {

        creditApplicationService.approve(id, request);
        return ResponseEntity.ok().build();
    }

    // 駁回已送審的授信申請。
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('REVIEWER')")
    @Operation(summary = "Reject credit application", description = "Rejects a SUBMITTED credit application. A rejection comment is required, and Maker-Checker prevents the application creator from rejecting their own application.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credit application rejected"),
            @ApiResponse(responseCode = "400", description = "Rejection comment is required", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "REVIEWER role required", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Credit application not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Business rule conflict, including invalid status, duplicate review, or Maker-Checker violation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> reject(
            @PathVariable Long id,
            @RequestBody RejectCreditApplicationRequest request) {

        creditApplicationService.reject(id, request);
        return ResponseEntity.ok().build();
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException("Page must be zero or greater");
        }

        if (size < 1) {
            throw new InvalidRequestException("Size must be greater than zero");
        }

        if (size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException(
                    "Size must not exceed " + MAX_PAGE_SIZE);
        }
    }
}
