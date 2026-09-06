package com.minghan.credit.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_limits")
public class CreditLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 一筆授信申請最多只能有一筆授信額度。
    // OneToOne 與 unique application_id 共同表達 Application 1:0..1 CreditLimit。
    // LAZY：只有真正需要 CreditApplication 資料時才載入，避免不必要查詢。
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private CreditApplication application;

    @Column(name = "limit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal limitAmount;

    @Column(name = "available_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // JPA 建立 Entity 時需要無參數建構子。
    protected CreditLimit() {
    }

    public CreditLimit(
            CreditApplication application,
            BigDecimal limitAmount) {
        this.application = application;
        this.limitAmount = limitAmount;
        // 額度剛建立時尚未動用，因此可用額度必須等於核准額度。
        this.availableAmount = limitAmount;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public CreditApplication getApplication() {
        return application;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public BigDecimal getAvailableAmount() {
        return availableAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
