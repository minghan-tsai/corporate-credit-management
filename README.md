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

**Stage 2 — 已完成**

目前已完成 Spring Boot 基礎架構與第一個可連線 PostgreSQL 的 Company Backend：

- 已建立 `corporate_credit_management` Database，Spring Boot DataSource 可正常連線 PostgreSQL，並已移除 Stage 1 暫時使用的 DataSource Auto-Configuration 排除。
- 已建立包含 `id`、`name`、`taxId` 與 `createdAt` 的 Company Entity，以及 Repository、Service、Create Request DTO 與 Controller。
- Hibernate 負責 Entity Mapping、CRUD 與 Schema Validation；正式 Schema 由 Flyway 管理，並設定 `spring.jpa.hibernate.ddl-auto=validate`。
- Flyway PostgreSQL Support 與 V1 Migration `V1__create_company_table.sql` 已完成，Database 已建立 `company` 與 `flyway_schema_history`。
- Create Request 對 `name` 與固定 8 碼的 `taxId` 提供基本 Validation。
- 已提供 `POST /api/companies`、`GET /api/companies/{id}` 與 `GET /api/companies`。
- 已使用 VS Code REST Client 人工驗證 Company API，GET／POST 均成功回應，並確認資料實際寫入 PostgreSQL。
- Maven `test` 與 `package` Lifecycle 均為 `BUILD SUCCESS`；目前尚未建立正式 Automated Test Classes。
- Health、Company API 與 `/error` 僅在開發期間暫時放行，CSRF 亦暫時關閉；正式 Authentication、JWT 與 RBAC 尚未完成。

目前尚未完成：

- CreditApplication／CreditReview
- CreditLimit／Drawdown
- 正式 Authentication／JWT／RBAC
- Business Rules
- Exception Handling
- Automated Tests
- AuditLog
- Swagger／OpenAPI
- Docker
- CI／GitHub Actions
