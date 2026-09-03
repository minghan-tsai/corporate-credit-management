package com.minghan.credit.service;

import com.minghan.credit.entity.Company;
import com.minghan.credit.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Company create(String name, String taxId) {
        // 由 Service 建立 Entity，再交由 Repository 持久化
        Company company = new Company(name, taxId);
        return companyRepository.save(company);
    }

    public Optional<Company> findById(Long id) {
        return companyRepository.findById(id);
    }

    public List<Company> findAll() {
        return companyRepository.findAll();
    }
}
