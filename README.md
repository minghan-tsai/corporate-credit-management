# Corporate Credit Management System

## 1. Project Overview

Corporate Credit Management System 是以 Java 與 Spring Boot 開發的 Backend Portfolio Project，模擬銀行內部企業授信管理流程。專案聚焦於可說明的 Business Rules、Transaction 一致性、RBAC、Maker-Checker 與 Audit Trail；範圍刻意維持小而深，不試圖重現完整的 Core Banking System。

## 2. Core Workflow

`Company → CreditApplication → Submit → Review → Approve / Reject → CreditLimit → Drawdown`

目前已完成 Company Backend／APIs、CreditApplication 建立與 Submit，以及 CreditReview、Approve／Reject 與 CreditLimit；下一階段為 Stage 5 Security／JWT／RBAC／Maker-Checker。

## 3. Current Progress

| Stage | Scope | Status |
| --- | --- | --- |
| Stage 0 | Project Setup | Completed |
| Stage 1 | Spring Boot Skeleton | Completed |
| Stage 2 | PostgreSQL／JPA／Flyway／Company API | Completed |
| Stage 3 | CreditApplication／Submit | Completed |
| Stage 4 | CreditReview／Approve／Reject／CreditLimit | Completed |
| Stage 5 | Security／JWT／RBAC／Maker-Checker | Next |

Stage 4 已於 2026-09-06 完成實作與人工驗證；後續進度請參考 [PROJECT_PLAN.md](PROJECT_PLAN.md)。

## 4. Implemented Features

- 可正常啟動的 Spring Boot Application 與 Layered Architecture。
- PostgreSQL DataSource Connection。
- Company 的 JPA／Hibernate Entity Mapping，以及新增與查詢功能。
- Flyway V1 Migration：`V1__create_company_table.sql`。
- Company Entity、`CompanyRepository` 與 `CompanyService`。
- `CreateCompanyRequest` DTO 與基本 Validation。
- `CompanyController`。
- CreditApplication Entity、Status Enum、Repository、Service 與 Controller。
- Company `1:N` CreditApplication；CreditApplication 端使用 LAZY `@ManyToOne`。
- `requestedAmount` 使用 `BigDecimal`，新申請初始狀態固定為 `DRAFT`。
- `CreateCreditApplicationRequest` 與 `CreditApplicationResponse` DTO；Response DTO 避免直接序列化 Hibernate LAZY Proxy。
- 只有 `DRAFT` 申請可以 Submit 為 `SUBMITTED`。
- CreditReviewDecision、CreditReview、CreditLimit Entities，以及對應的 Spring Data JPA Repositories。
- CreditApplication `1:N` CreditReview，以及 CreditApplication `1:0..1` CreditLimit；CreditLimit 的 `application_id` 具唯一限制。
- `ApproveCreditApplicationRequest` 與 `RejectCreditApplicationRequest` record DTO。
- Approve 僅接受 `SUBMITTED` 申請，核准金額必須大於零且不得超過申請金額；成功時建立 CreditReview 與 CreditLimit。
- Reject 僅接受 `SUBMITTED` 申請且原因不得為 null／blank；成功時建立 CreditReview，不建立 CreditLimit。
- CreditLimit 建立時 `availableAmount = limitAmount`。
- Approve／Reject 使用 Service `@Transactional`；Application 狀態由 managed Entity 的 JPA Dirty Checking 寫回。
- `POST /api/companies`。
- `GET /api/companies/{id}`。
- `GET /api/companies`。
- `POST /api/credit-applications`。
- `POST /api/credit-applications/{id}/submit`。
- `POST /api/credit-applications/{id}/approve`。
- `POST /api/credit-applications/{id}/reject`。
- Flyway V2 Migration：`V2__create_credit_applications_table.sql`。
- Flyway V3 Migration：建立 `credit_reviews` 與 `credit_limits`。
- VS Code REST Client 人工 API 驗證。
- PostgreSQL 實際寫入驗證。
- Maven `test` 與 `package` Lifecycle `BUILD SUCCESS`。

## 5. Architecture

`Controller → Service → Repository → PostgreSQL`

- API Request 使用 DTO。
- Entity 作為 Persistence Model。
- CreditApplication API 使用 Response DTO 與 Persistence Model 隔離。
- Business Logic 原則上放在 Service。
- Approve／Reject 的 Transaction Boundary 已放在 Service，確保狀態與 Review／Limit 一致更新。

## 6. Tech Stack

### Backend

- Java 21
- Spring Boot 3.5.x
- Maven

### Web

- Spring Web
- Spring Validation

### Persistence

- PostgreSQL
- Spring Data JPA
- Hibernate
- Flyway

### Security

- Spring Security Dependency
- 正式 Authentication、JWT 與 RBAC 尚未完成

### Testing

- Spring Boot Test Dependency
- Maven Lifecycle 驗證成功
- Stage 4 REST Client 與 PostgreSQL 人工驗證完成
- 正式 Automated Test Source 尚未建立

### Later Stages

- Stage 5：Security／JWT／RBAC／Maker-Checker
- Stage 6：Drawdown／Transaction
- Stage 7：Audit／Exception／Filtering／Pagination
- Stage 8：Automated Tests／CI
- Stage 9：Documentation／Final Verification
- Stage 10（Optional）：Docker／Docker Compose／One-command Startup
- Stage 11（Optional）：Public Deployment

## 7. API

| Method | Endpoint | Description | Status |
| --- | --- | --- | --- |
| `POST` | `/api/companies` | Create company | Implemented |
| `GET` | `/api/companies/{id}` | Get company by ID | Implemented |
| `GET` | `/api/companies` | Get all companies | Implemented |
| `POST` | `/api/credit-applications` | Create a DRAFT credit application | Implemented |
| `POST` | `/api/credit-applications/{id}/submit` | Submit a DRAFT application | Implemented |
| `POST` | `/api/credit-applications/{id}/approve` | Approve a SUBMITTED application | Implemented |
| `POST` | `/api/credit-applications/{id}/reject` | Reject a SUBMITTED application | Implemented |

## 8. Database & Migration

- Database 使用 PostgreSQL。
- Flyway 負責建立與修改正式 Schema。
- Hibernate 使用 `spring.jpa.hibernate.ddl-auto=validate` 驗證 Schema。
- V1 Migration 為 `V1__create_company_table.sql`。
- V1 已建立 `company`，Migration 紀錄保存於 `flyway_schema_history`。
- V2 Migration 為 `V2__create_credit_applications_table.sql`。
- V2 已建立 `credit_applications`，並以 Foreign Key `company_id` 參照 `company(id)`。
- V3 Migration 為 `V3__create_credit_reviews_and_credit_limits_tables.sql`。
- V3 已建立 `credit_reviews` 與 `credit_limits`；兩者皆以 `application_id` 參照 `credit_applications(id)`，且只有 `credit_limits.application_id` 具有 UNIQUE constraint。

## 9. Security Status

目前僅為 Development Configuration：

- Health、Company API、CreditApplication API 與 `/error` 暫時設為 `permitAll`。
- CSRF 暫時關閉。
- 正式 Authentication、JWT 與 RBAC 尚未完成。

## 10. Testing Status

已完成：

- VS Code REST Client 已人工驗證 Company API，以及 CreditApplication 建立與 Submit。
- PostgreSQL 已人工確認資料寫入與 `DRAFT → SUBMITTED` 狀態轉換。
- Stage 4 Approve 人工驗證：Application #4 由 9,000,000 核准 6,000,000，最終為 `APPROVED`，並建立 `APPROVED` Review 與 limit／available amount 均為 6,000,000 的 CreditLimit。
- Stage 4 Reject 人工驗證：Application #6（requestedAmount 5,000,000）最終為 `REJECTED`，Review 的 approvedAmount 為 NULL，且未建立 CreditLimit。
- DRAFT 直接 Approve、核准金額超過申請金額、空白 Reject comment，以及已 APPROVED 後再次 Approve 均已確認被 Business Rule 阻擋。
- 重複 Submit 已由 Business Rule 阻擋；Exception Handling 尚未完成，因此目前回傳 500。
- Stage 4 Business Rule 錯誤目前亦可能回傳 HTTP 500，正式 Exception Handling 留待後續 Stage。
- Maven `test` Lifecycle 驗證。
- Maven `package` Lifecycle 驗證。

尚未完成：

- Unit Tests
- Integration Tests
- Security Tests
- Transaction Tests

## 11. Roadmap

- Stage 5：Security／JWT／RBAC／Maker-Checker
- Stage 6：Drawdown／Transaction
- Stage 7：Audit／Exception／Filtering／Pagination
- Stage 8：Automated Tests／CI
- Stage 9：Documentation／Final Verification
- Stage 10（Optional）：Docker／Docker Compose／One-command Startup
- Stage 11（Optional）：Public Deployment；不得排擠 Business Logic、Security、Testing 等核心工作

## 12. Project Documents

完整 Scope、Domain Model、Business Rules、Architecture Rules、Testing Strategy 與 Stage Plan 請參考 [PROJECT_PLAN.md](PROJECT_PLAN.md)。
