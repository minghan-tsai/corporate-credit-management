package com.minghan.credit.repository;

import com.minghan.credit.entity.CreditLimit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// CreditLimit 的資料存取層，提供基本 CRUD 操作。
public interface CreditLimitRepository
        extends JpaRepository<CreditLimit, Long> {
    // 先做語意清楚的存在性檢查；資料庫 UNIQUE constraint 仍是最終一致性防線。
    boolean existsByApplicationId(Long applicationId);

    // 必須在 Transaction 內呼叫。資料庫 row lock 會讓同一筆額度的並行動用依序執行，
    // 後取得鎖的 Transaction 會看到前一筆扣減後的 availableAmount。
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select creditLimit from CreditLimit creditLimit where creditLimit.id = :id")
    Optional<CreditLimit> findByIdForUpdate(@Param("id") Long id);
}
