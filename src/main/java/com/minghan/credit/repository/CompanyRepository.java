package com.minghan.credit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minghan.credit.entity.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {

}
