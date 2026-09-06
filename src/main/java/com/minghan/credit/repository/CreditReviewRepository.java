package com.minghan.credit.repository;

import com.minghan.credit.entity.CreditReview;
import org.springframework.data.jpa.repository.JpaRepository;

// CreditReview 的資料存取層，提供基本 CRUD 操作。
public interface CreditReviewRepository
        extends JpaRepository<CreditReview, Long> {
    // Spring Data 會沿著 application.id 產生查詢，供 Service 阻擋重複審核。
    boolean existsByApplicationId(Long applicationId);
}
