package com.minghan.credit.repository;
import com.minghan.credit.entity.CreditApplication;
import com.minghan.credit.entity.CreditApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


// CreditApplication 的資料存取層，提供基本 CRUD 操作。
public interface CreditApplicationRepository
        extends JpaRepository<CreditApplication, Long> {

    Page<CreditApplication> findByStatus(
            CreditApplicationStatus status,
            Pageable pageable);
}
