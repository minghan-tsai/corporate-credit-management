package com.minghan.credit.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minghan.credit.dto.CreateCompanyRequest;
import com.minghan.credit.entity.Company;
import com.minghan.credit.exception.ApiErrorResponse;
import com.minghan.credit.service.CompanyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/companies")
@Tag(name = "Companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @Operation(summary = "Create company", description = "Creates a company using its name and 8-digit Taiwan tax ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Company.class))),
            @ApiResponse(responseCode = "400", description = "Invalid company data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public Company create(@Valid @RequestBody CreateCompanyRequest request) {
        return companyService.create(
                request.getName(),
                request.getTaxId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get company by ID", description = "Returns a company by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Company.class))),
            @ApiResponse(responseCode = "404", description = "Company not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public Company getById(@PathVariable Long id) {
        return companyService.getById(id);
    }

    @GetMapping
    @Operation(summary = "List companies", description = "Returns all companies.")
    @ApiResponse(responseCode = "200", description = "Companies returned successfully", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Company.class))))
    public List<Company> getAll() {
        return companyService.findAll();
    }
}
