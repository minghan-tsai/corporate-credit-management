# Corporate Credit Management System — Project Plan

## 1. Project Positioning

Corporate Credit Management System 是一個以作品集為導向的 Java／Spring Boot Backend，刻意以「小而深」的範圍模擬銀行內部企業授信系統。專案聚焦於企業客戶建檔、授信申請、獨立審核、建立核准額度，以及 Drawdown 的完整生命週期。本專案並非完整的 Core Banking System。

- Artifact／資料夾：`corporate-credit-management`
- Base package：`com.minghan.credit`
- Packaging：可執行 JAR
- 目標完成日：2026-09-10
- 交付原則：**小而深**
- 目標職缺：金融 IT，以及銀行法金／授信 Backend

專案目標：

- 展示實務 Java 21 與 Spring Boot Backend Engineering 能力。
- 建模有意義的企業授信 Business Rules，而非只有 CRUD。
- 採用 REST API 設計與清楚的 Layered Architecture。
- 使用 PostgreSQL 與明確、具版本控制的 Schema Migration。
- 展示 Transaction Boundary 與 rollback 保證。
- 依規劃 Stage 實作 Authentication、Authorization、RBAC 與 Maker-Checker 控制。
- 維護獨立的業務 Audit Trail。
- 透過適度的自動化與人工測試驗證核心規則。
- 僅在核心系統穩固後加入可重現的 Packaging 與 CI。

優先順序為可說明的 Business Logic、資料一致性、Security Boundary 與測試。Public Deployment 為選配，不得排擠上述優先事項。

## 2. Scope

### In Scope

- **Company** — 企業客戶。
- **CreditApplication** — 企業授信申請。
- **CreditReview** — 核准或駁回的審核紀錄。
- **CreditLimit** — 已核准的授信額度。
- **Drawdown** — 已核准額度的動用。
- **User** — 銀行內部系統使用者。
- **Role** — RM、REVIEWER、ADMIN 等系統角色。
- **AuditLog** — 業務操作 Audit 紀錄。

目前已完成 Company、CreditApplication 建立與 Submit、CreditReview、Approve／Reject、CreditLimit，以及 AppUser／Role、Authentication、JWT、RBAC 與 Maker-Checker；Drawdown 與 AuditLog 仍屬後續 Stage。

### Out of Scope

V1 不包含：

- 真實信用評分
- 財務報表分析
- 擔保品估值
- 聯徵整合
- Basel 計算
- IFRS 9
- 完整還款或利息模型
- 逾期管理
- 真實金流
- 複雜多階段簽核
- Redis
- Kafka
- Kubernetes
- Elasticsearch
- Microservices

## 3. Roles

### RM

規劃權限：

- 建立企業客戶。
- 建立授信申請。
- 修改自己擁有且狀態為 DRAFT 的申請。
- Submit 授信申請。
- 查詢授信案件。
- 建立 Drawdown。

RM 不得 Approve 或 Reject 授信申請。

### REVIEWER

規劃權限：

- 查詢授信申請。
- Approve 或 Reject 已 Submit 的授信申請。
- 查看 CreditReview 與已核准的 CreditLimit。

Maker-Checker 是一條獨立的 Business Rule，而不只是 RM 與 REVIEWER 被賦予不同 Endpoint 權限後的自然結果。即使 User 的 Role 允許執行 Review，Service 層在允許 Approve 或 Reject 前，仍必須驗證 reviewer 與 `CreditApplication.createdBy` 不是同一位 User。只依賴 Endpoint Authorization 並不足以落實 Segregation of Duties。

Stage 5 採用 AppUser 對單一 Role 的模型，角色包含 RM、REVIEWER 與 ADMIN；Maker-Checker 仍作為獨立於 Endpoint Authorization 的 Service Business Rule。

### ADMIN

規劃職責：

- 管理 User 與 Role。
- 查詢 AuditLog。

ADMIN 不是金融業務的 super user，不應自動取得不受限制的申請、審核、額度或 Drawdown 操作權限。

## 4. Domain Model

### Domain

1. **Company** — 企業客戶。
2. **CreditApplication** — 企業授信申請。
3. **CreditReview** — 核准或駁回的審核紀錄。
4. **CreditLimit** — 已核准的授信額度。
5. **Drawdown** — 已核准額度的動用。
6. **User** — 銀行內部系統使用者。
7. **Role** — 系統角色。
8. **AuditLog** — 業務操作 Audit 紀錄。

### Relationships

- Company `1:N` CreditApplication
- AppUser `1:N` CreditApplication（`createdBy`）
- CreditApplication `1:N` CreditReview
- CreditApplication `1:0..1` CreditLimit
- CreditLimit `1:N` Drawdown

目前 Company `1:N` CreditApplication、AppUser `1:N` CreditApplication、CreditApplication `1:N` CreditReview，以及 CreditApplication `1:0..1` CreditLimit 已完成。CreditApplication 的 Company／createdBy 與 CreditReview 的多對一關聯均使用 LAZY；CreditLimit 對 CreditApplication 使用 LAZY `@OneToOne`，並以 UNIQUE `application_id` 保證每筆申請最多一筆額度。CreditLimit `1:N` Drawdown 仍為規劃。

User 參照：

- `CreditApplication.createdBy`（已完成）
- `CreditReview.reviewedBy`
- `Drawdown.createdBy`
- `AuditLog.user`

其餘尚未實作 Domain 的 ownership、fetch、cascade、identifier、indexing、locking 與金額欄位設計，將在排定的 Stage 中進行決策與 Review，不預先假設。

## 5. Core Workflow

`Company → CreditApplication → Submit → Review → Approve / Reject → CreditLimit → Drawdown`

V1 授信申請狀態：

- `DRAFT`
- `SUBMITTED`
- `APPROVED`
- `REJECTED`

V1 合法狀態轉換：

- `DRAFT → SUBMITTED → APPROVED`
- `DRAFT → SUBMITTED → REJECTED`

V1 刻意排除複雜的多階段簽核。

## 6. Business Rules

### CreditApplication

- `requestedAmount` 必須大於零。
- 只有 DRAFT 的授信申請可以修改。
- 只有 DRAFT 的授信申請可以 Submit。
- 只有 SUBMITTED 的授信申請可以 Approve 或 Reject。

### CreditReview

- 只有 REVIEWER 可以審核授信申請。
- Maker-Checker／Segregation of Duties 是 Service 層的 Business Rule：即使 Role Authorization 允許 Review，reviewer 也不得與 `CreditApplication.createdBy` 是同一位 User。
- Endpoint Authorization 與 RBAC 檢查不能取代 reviewer 與建立者的身分驗證。
- 執行 Approve 時，`approvedAmount` 必須大於零。
- `approvedAmount` 不得大於 `requestedAmount`。
- Reject 必須填寫原因。

Stage 4 已完成 `SUBMITTED` 狀態檢查、重複 Review 防護、`approvedAmount > 0`、`approvedAmount <= requestedAmount`，以及 Reject comment 不得為 null／blank；Stage 5 已完成 REVIEWER Authorization 與 Maker-Checker。

### CreditLimit

- 只有 APPROVED 的 CreditApplication 才能建立 CreditLimit。
- 一個 CreditApplication 最多只能有一個 CreditLimit。
- `usedAmount` 不得大於 `approvedAmount`。

Stage 4 已完成由 Approve 流程建立唯一 CreditLimit，並在建立時令 `availableAmount = limitAmount`。`usedAmount`／Drawdown 規則仍保留至 Stage 6。

### Approve 與 Reject 的 Transaction Boundary

Stage 4 已完成的 Approve Transaction 包含：

1. 將 CreditApplication 從 `SUBMITTED` 轉換為 `APPROVED`。
2. 建立 CreditReview。
3. 建立該授信申請唯一的 CreditLimit。

`approve()` 使用 `@Transactional`；任何步驟失敗時，Application 狀態、CreditReview 與 CreditLimit 必須一起 rollback。Application 由 Repository 載入後是 managed Entity，狀態變更透過 JPA Dirty Checking 寫回。

Stage 4 已完成的 Reject Transaction 包含：

1. 將 CreditApplication 從 `SUBMITTED` 轉換為 `REJECTED`。
2. 建立 CreditReview。

`reject()` 使用 `@Transactional`，且不建立 CreditLimit；任何失敗都不得留下部分成功的 Application 狀態或 CreditReview。

AuditLog 尚未實作。待 Stage 7 加入後，Approve／Reject 的 AuditLog 必須納入同一個既有 Transaction Boundary。

### Drawdown

- `amount` 必須大於零。
- CreditLimit 必須有效且尚未過期。
- Drawdown 的 `amount` 不得大於 `availableAmount`。
- 成功執行 Drawdown 時，必須以 atomic 方式更新 CreditLimit、建立 Drawdown，並建立 AuditLog。
- 任何步驟失敗時，整個操作都必須 rollback。

## 7. Audit Strategy

V1 規劃的 Audit Actions：

- `CREATE_COMPANY`
- `CREATE_APPLICATION`
- `SUBMIT_APPLICATION`
- `APPROVE_APPLICATION`
- `REJECT_APPLICATION`
- `CREATE_DRAWDOWN`

AuditLog 至少包含：

- `userId`
- `action`
- `targetType`
- `targetId`
- `timestamp`

Application Log 是營運／診斷用途的 Log。AuditLog 則是記錄何人在何時對哪個 Target 執行相關操作的持久性業務紀錄。兩者是不同概念，不得視為可互換的資訊。

## 8. Architecture

規劃採用 Layered Architecture：

`Controller → Service → Repository → PostgreSQL`

並搭配 DTO、Entity、Validation、Exception 與 Security 等關注面向。V1 維持 Modular Monolith；Microservices 不在範圍內。

### Coding Rules

1. Controller 不得包含核心 Business Logic。
2. Controller 不得直接呼叫 Repository。
3. API Request 使用 DTO；CreditApplication 已使用 Response DTO 隔離 API Contract 與 Entity，其他 API 將依 Stage 持續完善。
4. Transaction Boundary 主要放在 Service。
5. 核心 Business Rules 必須有 Test。
6. Password、JWT 與 Secret 絕對不得寫入 Log。
7. 避免範圍過大或無意義的 try/catch。
8. 不得加入無法清楚說明用途的 Framework。

### Technology Baseline

- Java 21
- Maven
- Spring Boot 3.5.16，可執行 JAR
- Spring Web
- Spring Data JPA
- Spring Security
- JJWT
- Spring Validation
- PostgreSQL JDBC Driver
- Flyway Core 與 Flyway PostgreSQL Support
- Spring Boot Test Dependency

JWT Authentication 已於 Stage 5 完成；後續規劃使用 OpenAPI／Swagger、JUnit 5、Mockito 與 GitHub Actions。Docker Compose 保留至 Optional Stage 10。Testcontainers 為高優先選配加分項。Spring Batch 或 Scheduling 僅在後續有合理用途且時程允許時採用。

Lombok 不應大量依賴。Redis、Kafka、Kubernetes、Elasticsearch 與 Microservices 排除於 V1 範圍外。

### API Map

目前已實作 Login、Company APIs，以及 CreditApplication 的建立、Submit、Approve 與 Reject API；授信端點已套用 JWT、RBAC 與 Maker-Checker。查詢、Drawdown 與 Audit 相關 API 仍為規劃層級。

| 功能 | Method／Path | 主要 Role | 狀態 |
| --- | --- | --- | --- |
| Login | `POST /api/auth/login` | Anonymous | 已實作；成功簽發 JWT |
| 建立 Company | `POST /api/companies` | RM | 已實作；正式 RBAC 未完成 |
| 查詢 Company | `GET /api/companies`, `GET /api/companies/{id}` | 已授權的內部 User | 已實作；正式 RBAC 未完成 |
| 建立 CreditApplication | `POST /api/credit-applications` | RM | 已實作；初始狀態固定 `DRAFT`；自動記錄 `createdBy` |
| 更新自己擁有的 DRAFT | `PUT /api/credit-applications/{id}` | RM | 規劃 |
| Submit 授信申請 | `POST /api/credit-applications/{id}/submit` | RM | 已實作；僅允許 `DRAFT → SUBMITTED`；RBAC 已完成 |
| 查詢授信申請 | `GET /api/credit-applications` | RM, REVIEWER | 規劃 |
| Approve 授信申請 | `POST /api/credit-applications/{id}/approve` | REVIEWER | 已實作；RBAC／Maker-Checker 已完成 |
| Reject 授信申請 | `POST /api/credit-applications/{id}/reject` | REVIEWER | 已實作；RBAC／Maker-Checker 已完成 |
| 查看 CreditReview | `GET /api/credit-applications/{id}/reviews` | REVIEWER | 規劃 |
| 查看 CreditLimit | `GET /api/credit-applications/{id}/credit-limit` | REVIEWER 與已授權的業務 User | 規劃 |
| 建立 Drawdown | `POST /api/credit-limits/{id}/drawdowns` | RM | 規劃 |
| 管理 User／Role | `/api/admin/users`, `/api/admin/roles` | ADMIN | 規劃 |
| 查詢 AuditLog | `GET /api/admin/audit-logs` | ADMIN | 規劃 |

Company Create Request DTO 與基本 Validation 已完成。CreditApplication 已完成 Create、Approve、Reject Request DTO 與 Response DTO；Response DTO 避免直接序列化 Hibernate LAZY Proxy。Business Rule 錯誤目前仍可能回傳 HTTP 500，正式 Exception Handling、通用 Response Code、Filtering、Pagination、Idempotency、Concurrency Control 與 Error Contract 留待後續 Stage。

## 9. Database Strategy

目前已完成：

- PostgreSQL Database 與 Spring Boot DataSource Connection。
- Spring Data JPA、Jakarta Persistence／JPA 與 Hibernate Entity Mapping。
- Flyway Core 與 Flyway PostgreSQL Support。
- `V1__create_company_table.sql` Migration。
- `V2__create_credit_applications_table.sql` Migration。
- `V3__create_credit_reviews_and_credit_limits_tables.sql` Migration。
- `V4__create_app_user_table.sql` Migration。
- `V5__add_created_by_to_credit_applications.sql` Migration。
- `company`、`app_user`、`credit_applications`、`credit_reviews`、`credit_limits` 與 `flyway_schema_history`。
- `credit_applications.company_id` Foreign Key 參照 `company(id)`。
- `credit_applications.created_by` Foreign Key 參照 `app_user(id)`。
- `credit_reviews.application_id` Foreign Key 參照 `credit_applications(id)`，且不設 UNIQUE，以支援 CreditApplication `1:N` CreditReview。
- `credit_limits.application_id` Foreign Key 參照 `credit_applications(id)`，並以 UNIQUE constraint 保證 CreditApplication `1:0..1` CreditLimit。
- CreditApplication 的 `requested_amount` 使用 `NUMERIC(19, 2)`，對應 Java `BigDecimal`。
- CreditReview 的 `approved_amount` 與 CreditLimit 的金額欄位均使用 `NUMERIC(19, 2)`，對應 Java `BigDecimal`。
- `spring.jpa.hibernate.ddl-auto=validate`。

原則：

- Flyway 負責建立與修改 Database Schema。
- Hibernate 負責 Object-Relational Mapping、Persistence Operations 與 Schema Validation。
- 正式 Schema 不由 Hibernate 自動建立。
- `ddl-auto=update` 不得作為正式的 Schema Management Strategy。
- Database Connection Details 必須外部化，Secret 不得 commit。

## 10. Security Strategy

### Current

- AppUser 使用單一 Role（RM、REVIEWER、ADMIN），密碼以 BCrypt 保存並由 UserDetailsService 載入。
- Login API 透過 AuthenticationManager 驗證帳密，成功後簽發以 username 為 subject 的 JWT。
- JWT Secret 由外部設定提供；JWT Filter 驗證 signature 與 expiration，並建立 Spring SecurityContext。
- Session 採 Stateless，`/api/auth/login` 維持 `permitAll`，授信流程需通過 Authentication。
- Method-level Security 以 `@PreAuthorize` 限制 RM 建立／Submit、REVIEWER Approve／Reject；ADMIN 不自動取得授信流程權限。
- Service 以 `CreditApplication.createdBy` 比對目前登入者，禁止 maker Approve／Reject 自己的案件。
- `manual-test` profile 使用 CommandLineRunner 初始化 BCrypt 測試帳號，不以測試 seed 污染 Flyway migration history。

## 11. Testing Strategy

### Current Status

- 尚未建立正式 Automated Test Classes。
- Maven `test` 與 `package` Lifecycle 均為 `BUILD SUCCESS`。
- VS Code REST Client 人工 Company API 驗證已完成。
- VS Code REST Client 已人工驗證 CreditApplication 建立與 Submit 成功。
- PostgreSQL 已人工確認 CreditApplication 資料寫入，以及 `DRAFT → SUBMITTED` 狀態轉換。
- 重複 Submit 已由 Business Rule 阻擋；因 Exception Handling 尚未完成，目前仍回傳 500，預計於後續 Stage 統一處理。
- Stage 4 REST Client 與 PostgreSQL 人工驗證已完成：Application #4 由 requestedAmount 9,000,000 核准 6,000,000，最終狀態為 `APPROVED`，CreditReview decision 為 `APPROVED`，CreditLimit 的 limitAmount 與 availableAmount 均為 6,000,000。
- Reject 人工驗證已完成：Application #6 的 requestedAmount 為 5,000,000，最終狀態為 `REJECTED`，CreditReview decision 為 `REJECTED`、approvedAmount 為 NULL，且未建立 CreditLimit。
- DRAFT 直接 Approve、approvedAmount 大於 requestedAmount、Reject comment 空白，以及已 APPROVED 再次 Approve 均已人工確認被阻擋。
- Stage 4 Business Rule 錯誤目前仍可能回傳 HTTP 500；正式 Exception Handling 留待 Stage 7。
- Stage 5 已以 manual-test profile 與 VS Code REST Client 人工驗證 Login、JWT、未登入／無效 JWT、RM／REVIEWER RBAC 與 Maker-Checker。

### Unit Test

- 使用 JUnit 5 與 Mockito。
- 主要聚焦於 Service Business Rules 與 State Transition。

### Integration Test

- 使用 Spring Boot Test。
- 涵蓋核心 API、Repository 與 Database Flow。

### Security Test

至少驗證：

- 未登入的 Request 目前回傳 403；正式 Authentication / Authorization Error Contract 將於 Stage 7 統一處理。
- RM 呼叫 Approve 回傳 403。
- REVIEWER 合法執行 Approve 時成功。

### Transaction Test

Stage 4 現階段 Approve 任一步驟失敗時，驗證以下項目必須一併 rollback：

- CreditApplication 不得錯誤地停留在 APPROVED。
- CreditReview 不得只完成部分 insert。
- CreditLimit 不得只完成部分 insert。

Stage 4 現階段 Reject 任一步驟失敗時，驗證以下項目：

- CreditApplication 不得錯誤地停留在 REJECTED。
- CreditReview 不得只完成部分 insert。

AuditLog 尚未實作；待 Stage 7 完成後，再將 AuditLog rollback 驗證納入 Approve／Reject Transaction Test。

Drawdown 失敗時，驗證以下所有項目：

- CreditLimit 不得被錯誤更新。
- Drawdown 不得 insert。
- AuditLog 不得 insert。

Testcontainers 是高優先加分項，但若時程壓力需要可省略。本專案不追求 100% Coverage；目標是充分涵蓋具風險的規則與 Boundary。

## 12. Delivery／CI 與 Optional Deployment

- 前期開發：Local Java 加 Local PostgreSQL。
- 核心交付：可執行 JAR、文件、測試與最終驗證。
- CI Platform：GitHub Actions。
- 最低 CI Commands：`mvn test` 與 `mvn package`。
- Docker／Docker Compose one-command startup 保留至 Optional Stage 10。
- Public Deployment 保留至 Optional Stage 11。

Deployment 工作不得排擠 Business Logic、Transaction 正確性、Security 或 Testing。

## 13. Development Stages

| Stage | Scope | Status | Actual Completion |
| --- | --- | --- | --- |
| Stage 0 | Project Planning／Initialization | Completed | 2026-08-22 |
| Stage 1 | Spring Boot Skeleton | Completed | 2026-08-25 |
| Stage 2 | PostgreSQL／JPA／Flyway／Company API | Completed | 2026-09-03 |
| Stage 3 | CreditApplication／Submit | Completed | 2026-09-04 |
| Stage 4 | CreditReview／Approve／Reject／CreditLimit | Completed | 2026-09-06 |
| Stage 5 | Security／JWT／RBAC／Maker-Checker | Completed | 2026-09-07 |
| Stage 6 | Drawdown／Transaction | Next | - |
| Stage 7 | Audit／Exception／Filtering／Pagination | Planned | - |
| Stage 8 | Automated Tests／CI | Planned | - |
| Stage 9 | Documentation／Final Verification | Planned | - |
| Stage 10 | Docker／Docker Compose／One-command Startup | Optional | - |
| Stage 11 | Public Deployment | Optional | - |

Stage 0～9 為核心必做；Stage 10～11 為 Optional，不得排擠 Business Logic、Security、Testing 等核心工作。

### Stage 10 — Docker／Docker Compose／One-command Startup（Optional）

- 建立 Spring Boot Dockerfile。
- 使用 Docker Compose 啟動 Spring Boot 與 PostgreSQL。
- 以 Environment Variables 提供資料庫連線與必要設定。
- 啟動時由 Flyway 執行版本化 Migration。
- README 提供一鍵啟動與必要環境設定說明。

### Stage 11 — Public Deployment（Optional）

- 將完成的核心系統部署至 Public Environment。
- 僅在 Stage 0～9 核心工作穩定完成後進行。
- 不得排擠 Business Logic、Security、Testing 或資料一致性工作。

### Original Schedule

原始規劃日期皆為 2026 年，時區為 Asia/Taipei；每個開發日預計投入約 5～6 小時，目標完成日維持 2026-09-10。

- Stage 0：8/21–8/22
- Stage 1：8/23–8/25
- Stage 2：原訂 8/26，實際完成日為 9/3
- Stage 3：原訂 8/27，實際完成日為 9/4
- Stage 4：原訂 8/28–8/29
- Stage 5：原訂 8/30–8/31
- 9/1–9/2：不安排電腦開發
- Stage 6：原訂 9/3–9/4
- Stage 7：原訂 9/5
- Stage 8：原訂 9/6–9/7
- Stage 9：原訂 9/8–9/10

Stage 10 與 Stage 11 為後續新增的 Optional Roadmap，不設定核心交付期限。

2026-09-08 後不得新增大型功能。

## 14. Current Status

**Current Stage：Stage 5 Completed**

**Next Stage：Stage 6 — Drawdown／Transaction**

- AppUser／Role 與對應 Repository、Flyway V4／V5 Migration 已完成；CreditApplication 會保存建立者 `createdBy`。
- BCrypt PasswordEncoder、UserDetailsService、AuthenticationManager 與 Login API 已完成。
- JWT 產生／驗證、JWT Authentication Filter、SecurityContext 建立與 Stateless Session 已完成。
- Method-level RBAC 已限制 RM 建立／Submit，以及 REVIEWER Approve／Reject；ADMIN 不作為授信流程 super user。
- Maker-Checker 已在 Service 層阻擋建立者 Approve／Reject 自己的案件。
- `manual-test` profile 以 CommandLineRunner 建立本機 BCrypt 測試帳號，並已完成人工 Security 整合驗證。
- Maven `test`／`package` Lifecycle 均為 `BUILD SUCCESS`，但尚無正式 Automated Test Source。
- Business Rule 錯誤目前仍可能回傳 HTTP 500；正式 Exception Handling 留待 Stage 7。
- Stage 5 完成日為 2026-09-07；下一階段為 Stage 6：Drawdown／Transaction。

## 15. Definition of Done

每個 Stage 至少必須確認：

1. 已完成約定 Stage Scope 內的 Code。
2. Business Rules 已驗證。
3. 核心 Test 通過。
4. Manual Verification 通過。
5. `PROJECT_PLAN.md` 已更新。
6. Build 通過。
7. Git Status 已確認。

規劃的 Stage 收尾流程：

1. `git diff`
2. `git diff --check`
3. `mvn test`
4. `mvn package`
5. `git status`
6. Commit
7. Push
8. 驗證 CI
9. 必要時建立 Tag

Stage Tags 為 `v1-stage-0` 至 `v1-stage-9`。最終 Release Tag 為 `v1.0.0`。

## 16. AI／Codex Collaboration Rules

AI／Codex 協作以完成作品、理解架構與可驗證的學習流程並重。

- 每個 Stage 固定先學習必要基礎概念，再確認設計、Business Rule 與驗證方式。
- AI／Codex 可以協助產生第一版程式碼與 Boilerplate，以加速作品完成。
- 不要求使用者第一次接觸核心技術時，必須從空白自行默寫完整第一版。
- 目前優先目標為完成作品，並熟悉 Spring Boot 架構與核心邏輯；語法手寫與 Live Coding 熟練度於後續模擬面試階段加強。
- 未經明確同意，Codex 不得導入大型 Framework 或改變 Architecture。
- 修改核心 Code 前，Codex 必須說明修改目的。
- 修改後，Codex 必須列出變更檔案與每項變更原因。
- 使用者必須 Review 所有核心 Diff。

以下核心領域可由 AI／Codex 協助產生第一版，但不得以黑箱方式完成：

- JPA Relationship
- Transaction
- Security
- Business Rule
- Exception
- Test

使用者必須對上述核心領域逐段理解、Review 並驗證；AI／Codex 應清楚說明設計理由、風險與驗證結果，而不是只交付可執行程式碼。
