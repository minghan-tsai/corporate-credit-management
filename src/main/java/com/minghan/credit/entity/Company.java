package com.minghan.credit.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    // 公司統編不可為空且不可重複
    @Column(name = "tax_id", nullable = false, unique = true, length = 8)
    private String taxId;
    // 建立後不可修改建立時間
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Entity 第一次寫入資料庫前，自動設定建立時間
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // 提供 Service 建立新的 Company Entity
    public Company(String name, String taxId) {
        this.name = name;
        this.taxId = taxId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTaxId() {
        return taxId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // JPA / Hibernate 建立 Entity 時需要無參數建構子
    protected Company() {
    }
}
