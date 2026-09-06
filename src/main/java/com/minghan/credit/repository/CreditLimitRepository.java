package com.minghan.credit.repository;

import com.minghan.credit.entity.CreditLimit;
import org.springframework.data.jpa.repository.JpaRepository;

// CreditLimit 的資料存取層，提供基本 CRUD 操作。
public interface CreditLimitRepository
        extends JpaRepository<CreditLimit, Long> {
    // 先做語意清楚的存在性檢查；資料庫 UNIQUE constraint 仍是最終一致性防線。
    boolean existsByApplicationId(Long applicationId);
}
