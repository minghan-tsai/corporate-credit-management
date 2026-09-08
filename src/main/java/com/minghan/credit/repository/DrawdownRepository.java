package com.minghan.credit.repository;

import com.minghan.credit.entity.Drawdown;
import org.springframework.data.jpa.repository.JpaRepository;

// Drawdown 的資料存取層，提供基本 CRUD 操作。
public interface DrawdownRepository extends JpaRepository<Drawdown, Long> {
}
