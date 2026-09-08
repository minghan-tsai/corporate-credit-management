package com.minghan.credit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "drawdowns")
public class Drawdown {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 一筆 CreditLimit 可以有多筆 Drawdown。
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_limit_id", nullable = false)
    private CreditLimit creditLimit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    // 一位 AppUser 可以建立多筆 Drawdown。
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // JPA 建立 Entity 時需要無參數建構子。
    protected Drawdown() {
    }

    public Drawdown(
            CreditLimit creditLimit,
            BigDecimal amount,
            AppUser createdBy) {
        this.creditLimit = creditLimit;
        this.amount = amount;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public CreditLimit getCreditLimit() {
        return creditLimit;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public AppUser getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
