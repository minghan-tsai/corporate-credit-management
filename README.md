# Corporate Credit Management System

## 1. Project Overview

Corporate Credit Management System 是以 Java 與 Spring Boot 開發的 Backend Portfolio Project，模擬銀行內部企業授信管理流程。專案聚焦於可說明的 Business Rules、Transaction 一致性、RBAC、Maker-Checker 與 Audit Trail；範圍刻意維持小而深，不試圖重現完整的 Core Banking System。

## 2. Core Workflow

`Company → CreditApplication → Submit → Review → Approve / Reject → CreditLimit → Drawdown`

目前已完成 Company Backend／APIs、CreditApplication 建立與審核流程、登入／JWT／RBAC／Maker-Checker、Drawdown 額度動用、全域錯誤處理、Audit Trail、Filtering／Pagination，以及 Stage 8 Automated Tests／CI 的實作與本機驗證；下一階段為 Stage 9 Documentation／Final Verification。

## 3. Current Progress

| Stage | Scope | Status |
| --- | --- | --- |
| Stage 0 | Project Setup | Completed |
| Stage 1 | Spring Boot Skeleton | Completed |
| Stage 2 | PostgreSQL／JPA／Flyway／Company API | Completed |
| Stage 3 | CreditApplication／Submit | Completed |
| Stage 4 | CreditReview／Approve／Reject／CreditLimit | Completed |
| Stage 5 | Security／JWT／RBAC／Maker-Checker | Completed |
| Stage 6 | Drawdown／Transaction | Completed |
| Stage 7 | Audit／Exception／Filtering／Pagination | Completed |
| Stage 8 | Automated Tests／CI | Implementation／Local Verification Completed；Remote CI Pending |
| Stage 9 | Documentation／Final Verification | Next |

Stage 8 已完成測試補強與 CI workflow 設定，本機 59 tests 全數通過；GitHub Actions 待 push 後進行遠端驗證。後續進度請參考 [PROJECT_PLAN.md](PROJECT_PLAN.md)。

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
- AppUser／Role、BCrypt PasswordEncoder 與 Spring Security UserDetailsService。
- Login API、JWT Authentication 與 Stateless Security。
- RM／REVIEWER RBAC，以及以 `CreditApplication.createdBy` 落實的 Maker-Checker。
- Flyway V4／V5 Migration：建立 `app_user`，並為授信申請加入 `created_by`。
- Drawdown Entity、Request／Response DTO、Repository、Service 與 Controller；CreditLimit `1:N` Drawdown，並以 JWT current user 記錄 `createdBy`。
- `POST /api/credit-limits/{creditLimitId}/drawdowns` 僅允許 RM；REVIEWER 呼叫回傳 403。
- Drawdown 使用 `@Transactional` 與 `PESSIMISTIC_WRITE` 鎖定 CreditLimit，扣減 `availableAmount` 後由 JPA Dirty Checking 寫回。
- Drawdown 金額不得為 null、0、負數或超過可用額度；基本輸入錯誤回傳 400、CreditLimit 不存在回傳 404、超額動用回傳 409 Conflict。
- Flyway V6 Migration：建立 `drawdowns`、CreditLimit／AppUser Foreign Keys 與索引。
- `@RestControllerAdvice`／`@ExceptionHandler` 與統一 `ApiErrorResponse`，將 input、not-found、business conflict 分別映射為 400、404、409，未預期錯誤維持安全的 500 response。
- `AuditLog` 以 `AuditAction`／`AuditEntityType` enum 記錄 actor 與五項核心 business actions；`Propagation.MANDATORY` 確保 Audit 與原 business transaction 一起 commit／rollback。
- `GET /api/credit-applications` 支援 status filtering、pagination 與標準 Spring Data sorting，並在資料庫層完成分頁、以 Response DTO 回傳。
- Page／size validation 與最大 page size 100 防護。
- Flyway V7 Migration：建立 `audit_logs` 與 entity lookup index。
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
- Drawdown 的 Transaction Boundary 位於 Service，並以 Pessimistic Lock 防止同一額度並行超額動用。
- Audit write 加入相同的 business transaction，失敗時不會留下宣稱成功的 AuditLog。

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

- Spring Security
- BCrypt PasswordEncoder／UserDetailsService
- Login API／JWT Authentication／Stateless Session
- Method-level RBAC／Maker-Checker

### Testing

- Spring Boot Test Dependency
- Spring Security Test Dependency
- Maven Lifecycle 驗證成功
- Stage 4～Stage 7 REST Client 人工驗證完成
- Stage 8 累計 59 個 automated tests，本機 Maven `test` 為 0 failures／0 errors／0 skipped，`BUILD SUCCESS`
- GitHub Actions CI workflow 已設定 push／pull request、JDK 21、Maven test／package 與 dependency cache，待遠端驗證

### Later Stages

- Stage 5：Security／JWT／RBAC／Maker-Checker（Completed）
- Stage 6：Drawdown／Transaction（Completed）
- Stage 7：Audit／Exception／Filtering／Pagination（Completed）
- Stage 8：Automated Tests／CI（Implementation／Local Verification Completed；Remote CI Pending）
- Stage 9：Documentation／Final Verification（Next）
- Stage 10（Optional）：Docker／Docker Compose／One-command Startup
- Stage 11（Optional）：Public Deployment

## 7. API

| Method | Endpoint | Description | Status |
| --- | --- | --- | --- |
| `POST` | `/api/auth/login` | Authenticate and issue JWT | Implemented |
| `POST` | `/api/companies` | Create company | Implemented |
| `GET` | `/api/companies/{id}` | Get company by ID | Implemented |
| `GET` | `/api/companies` | Get all companies | Implemented |
| `GET` | `/api/credit-applications?status=&page=&size=&sort=` | Filter and page credit applications | Implemented |
| `POST` | `/api/credit-applications` | Create a DRAFT credit application | Implemented |
| `POST` | `/api/credit-applications/{id}/submit` | Submit a DRAFT application | Implemented |
| `POST` | `/api/credit-applications/{id}/approve` | Approve a SUBMITTED application | Implemented |
| `POST` | `/api/credit-applications/{id}/reject` | Reject a SUBMITTED application | Implemented |
| `POST` | `/api/credit-limits/{creditLimitId}/drawdowns` | Create a drawdown from available credit limit | Implemented |

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
- V4 Migration 建立 `app_user`，V5 Migration 為 `credit_applications` 加入 `created_by` Foreign Key。
- V6 Migration 建立 `drawdowns`，並以 Foreign Key 參照 `credit_limits` 與 `app_user`。
- V7 Migration 建立 `audit_logs`，保存 actor username、action、entity type／ID 與建立時間。

## 9. Security Status

Stage 5 已完成基本 Security Boundary：

- Login API 驗證帳密並簽發 JWT；Bearer token 由 JWT Filter 驗證後建立 SecurityContext。
- Session 採 Stateless，CSRF 關閉；`/api/auth/login` 可匿名存取，授信流程需通過 Authentication 與 method-level RBAC。
- RM 可建立與 Submit 授信申請，REVIEWER 可 Approve／Reject；Service 層 Maker-Checker 禁止建立者審核自己的案件。
- Drawdown API 僅允許 RM；Service 從 JWT SecurityContext 取得目前使用者並記錄為 `createdBy`。
- Security Filter Chain 已設定 AuthenticationEntryPoint：未登入或無效／過期 JWT 回傳 401，已登入但權限不足回傳 403。
- Health、Company API 與 `/error` 仍維持目前開發階段的 `permitAll` 設定。

## 10. Testing Status

已完成：

- VS Code REST Client 已人工驗證 Company API，以及 CreditApplication 建立與 Submit。
- PostgreSQL 已人工確認資料寫入與 `DRAFT → SUBMITTED` 狀態轉換。
- Stage 4 Approve 人工驗證：Application #4 由 9,000,000 核准 6,000,000，最終為 `APPROVED`，並建立 `APPROVED` Review 與 limit／available amount 均為 6,000,000 的 CreditLimit。
- Stage 4 Reject 人工驗證：Application #6（requestedAmount 5,000,000）最終為 `REJECTED`，Review 的 approvedAmount 為 NULL，且未建立 CreditLimit。
- DRAFT 直接 Approve、核准金額超過申請金額、空白 Reject comment，以及已 APPROVED 後再次 Approve 均已確認被 Business Rule 阻擋。
- 重複 Submit 已由 Business Rule 阻擋；Stage 7 全域錯誤處理會回傳 409 Conflict。
- Stage 4 原有的 Business Rule 錯誤已於 Stage 7 納入一致的 400／404／409 error contract。
- Stage 5 已以 manual-test profile 與 VS Code REST Client 人工驗證 Login、JWT、RBAC 與 Maker-Checker。
- Stage 6 已人工驗證 RM 正常建立 Drawdown（201）、amount = 0（400）、超額（409 Conflict），以及 REVIEWER 權限阻擋（403）。
- `DrawdownServiceTest` 已補 amount 等於 availableAmount 的邊界測試，驗證成功動用後剩餘額度為 0。
- Maven `test` Lifecycle 驗證。
- Maven `package` Lifecycle 驗證。
- Stage 7 automated tests 共 29 個，涵蓋全域錯誤格式、Audit action／transaction boundary、CreditApplication filtering／pagination，以及 Drawdown business conflict。
- Stage 8 補強 `CreditApplicationServiceTest` 的 Maker-Checker、Approve／Reject 狀態、核准金額、重複 Review／CreditLimit 與 Reject reason 規則。
- Stage 8 使用 Spring Security Test 經實際 Security Filter Chain 與 `@PreAuthorize` 明確驗證 anonymous／invalid JWT 回傳 401、已登入但角色不足回傳 403，以及 RM／REVIEWER／ADMIN RBAC；測試曾發現 anonymous request 原本回傳 403，加入 AuthenticationEntryPoint 後修正為 401。
- 目前累計 59 tests；本機 Maven `test` 為 0 failures／0 errors／0 skipped，`BUILD SUCCESS`。
- `.github/workflows/ci.yml` 已完成 push／pull request CI 設定，使用 Ubuntu、JDK 21、Maven Wrapper、dependency cache、`test` 與 `package`；待 push 後進行遠端驗證。
- Stage 7 Manual Test 已驗證 pagination、filtering、sorting、400／404／409、Approve／Reject／Drawdown Audit Trail、失敗 action 不產生成功 Audit，以及超額 Drawdown 後 availableAmount 不變。
- `git diff --check` PASS。

尚未完成：

- Testcontainers／Database Integration Tests
- 真實資料庫 Transaction Rollback Tests
- Drawdown Concurrency Integration Tests
- GitHub Actions Remote Verification

## 11. Roadmap

- Stage 5：Security／JWT／RBAC／Maker-Checker（Completed：2026-09-07）
- Stage 6：Drawdown／Transaction（Completed：2026-09-08）
- Stage 7：Audit／Exception／Filtering／Pagination（Completed：2026-09-09）
- Stage 8：Automated Tests／CI（Implementation／Local Verification Completed；Remote CI Pending）
- Stage 9：Documentation／Final Verification（Next）
- Stage 10（Optional）：Docker／Docker Compose／One-command Startup
- Stage 11（Optional）：Public Deployment；不得排擠 Business Logic、Security、Testing 等核心工作

## 12. Project Documents

完整 Scope、Domain Model、Business Rules、Architecture Rules、Testing Strategy 與 Stage Plan 請參考 [PROJECT_PLAN.md](PROJECT_PLAN.md)。
