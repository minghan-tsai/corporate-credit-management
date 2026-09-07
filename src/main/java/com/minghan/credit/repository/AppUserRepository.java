package com.minghan.credit.repository;

import com.minghan.credit.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// AppUser 的資料存取層，提供基本 CRUD 與 username 查詢。
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
}
