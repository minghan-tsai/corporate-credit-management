package com.minghan.credit.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_reviews")
public class CreditReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 一筆授信申請可以有多筆審核紀錄。
    // ManyToOne 對應 Application 1:N Review，因此 application_id 不可設為唯一。
    // LAZY：只有真正需要 CreditApplication 資料時才載入，避免不必要查詢。
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private CreditApplication application;

    // STRING 儲存 APPROVED／REJECTED 名稱，而非容易因 enum 排序改變而失真的 ordinal。
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CreditReviewDecision decision;

    // Reject 時沒有核准金額，因此允許 approvedAmount 為 null。
    @Column(name = "approved_amount", precision = 19, scale = 2)
    private BigDecimal approvedAmount;

    @Column
    private String comment;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    // JPA 建立 Entity 時需要無參數建構子。
    protected CreditReview() {
    }

    public CreditReview(
            CreditApplication application,
            CreditReviewDecision decision,
            BigDecimal approvedAmount,
            String comment) {
        this.application = application;
        this.decision = decision;
        this.approvedAmount = approvedAmount;
        this.comment = comment;
        // 審核時間由後端建立，避免 API 呼叫端自行指定審核發生時間。
        this.reviewedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public CreditApplication getApplication() {
        return application;
    }

    public CreditReviewDecision getDecision() {
        return decision;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }
}
