package com.minghan.credit.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minghan.credit.dto.CreateCompanyRequest;
import com.minghan.credit.entity.Company;
import com.minghan.credit.service.CompanyService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    public Company create(@Valid @RequestBody CreateCompanyRequest request) {
        return companyService.create(
                request.getName(),
                request.getTaxId());
    }

    @GetMapping("/{id}")
    public Company getById(@PathVariable Long id) {
        return companyService.findById(id)
                .orElseThrow();
    }

    @GetMapping
    public List<Company> getAll() {
        return companyService.findAll();
    }
}
