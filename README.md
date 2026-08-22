# Corporate Credit Management System

## 專案目的

Corporate Credit Management System 是一個以 Java 與 Spring Boot 開發的作品集專案，模擬銀行內部企業授信流程中一個聚焦的範圍。本專案旨在展示 Business Rule 設計、Layered Architecture、Transaction 完整性、授權控管、可稽核性與自動化測試，但不試圖重現完整的銀行核心系統。

## 規劃功能

- 企業客戶管理
- 授信申請草稿與送審
- 具備 Maker-Checker 控制的審核核准或駁回
- 核准授信額度管理
- 具備 Transaction 一致性的 Drawdown 處理
- RM、REVIEWER 與 ADMIN 的 RBAC
- 業務 Audit Trail
- Validation、Exception Handling、Filtering 與 Pagination
- Unit Test、Integration Test、Security Test 與 Transaction Test

## 規劃 Tech Stack

- Java 21
- Spring Boot 3.x 與 Maven
- Spring Web、Spring Data JPA、Spring Security 與 Spring Validation
- PostgreSQL、Hibernate 與 Flyway
- JUnit 5、Mockito、Spring Boot Test，以及選配的 Testcontainers
- 後期階段：Docker Compose 與 GitHub Actions

## 目前狀態

**Stage 0 — 已完成**

目前僅完成專案規劃、開發環境、最小化 Spring Boot Application Skeleton 與 Build Configuration。尚未開始 Stage 1 業務功能實作，也尚未實作任何正式 Domain、API、Security Flow、Database Migration、Docker 設定或 CI Workflow。
