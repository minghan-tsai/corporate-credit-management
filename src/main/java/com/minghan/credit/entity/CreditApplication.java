package com.minghan.credit.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_applications")
public class CreditApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 一家公司可以有多筆授信申請。
    // LAZY：只有真正需要 Company 資料時才載入，避免不必要查詢。
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // 金融金額使用 BigDecimal，避免 double 浮點精度誤差。
    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal requestedAmount;

    @Column(nullable = false, length = 500)
    private String purpose;

    // Enum 以字串形式存入 DB，例如 DRAFT、SUBMITTED。
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CreditApplicationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // JPA 建立 Entity 時需要無參數建構子。
    protected CreditApplication() {
    }

    public CreditApplication(
            Company company,
            BigDecimal requestedAmount,
            String purpose) {
        this.company = company;
        this.requestedAmount = requestedAmount;
        this.purpose = purpose;
        
        // 新建立的授信申請固定從 DRAFT 開始，
        // 不允許由 API 呼叫端自行指定初始狀態。
        this.status = CreditApplicationStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public String getPurpose() {
        return purpose;
    }

    public CreditApplicationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // 執行送審動作，由 DRAFT 轉為 SUBMITTED。
    public void submit() {
        this.status = CreditApplicationStatus.SUBMITTED;
    }
}
