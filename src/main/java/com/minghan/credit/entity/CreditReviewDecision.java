package com.minghan.credit.entity;

// 授信審核的決策結果。
// 與 Application 流程狀態分開建模，讓審核紀錄保有自己的決策語意。
public enum CreditReviewDecision {
    APPROVED,
    REJECTED
}
