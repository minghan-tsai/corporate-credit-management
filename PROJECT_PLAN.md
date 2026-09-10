# Corporate Credit Management System — 工程計畫與目前實作紀錄

## 1. Project Positioning

Corporate Credit Management System 是一個以作品集為導向的 Java／Spring Boot Backend，刻意以「小而深」的範圍模擬銀行內部企業授信系統。專案聚焦於企業客戶建檔、授信申請、獨立審核、建立核准額度，以及 Drawdown 的完整生命週期。本專案並非完整的 Core Banking System。

本文件同時保存工程計畫與目前實作紀錄。為避免把 roadmap 誤認為已交付功能，狀態統一使用以下語意：

- **已實作**：目前 production code／schema 已存在。
- **已自動化驗證**：已有 automated test 或 CI 證據。
- **已手動驗證**：已有 REST Client／PostgreSQL 實際驗證證據。
- **Planned**：V1 原始規劃中尚未實作的項目。
- **Optional**：不屬於 Stage 0～10 核心交付的加分項。

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

目前已完成 Company、CreditApplication 建立與 Submit、CreditReview、Approve／Reject、CreditLimit、Drawdown，以及 AppUser／Role、Authentication、JWT、RBAC、Maker-Checker 與 AuditLog；Stage 7 另完成全域錯誤處理及 CreditApplication 查詢分頁，Stage 9 已補齊建立申請時的 `requestedAmount` 驗證，Stage 10 已加入 OpenAPI／Swagger UI。Company API 的正式 RBAC 與 AuditLog 查詢 API 尚未實作。

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

目標權限；目前 Company API 仍為 public，其餘狀態依 API Map：

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

目前 Company `1:N` CreditApplication、AppUser `1:N` CreditApplication、CreditApplication `1:N` CreditReview、CreditApplication `1:0..1` CreditLimit，以及 CreditLimit `1:N` Drawdown 均已實作。CreditReview 的 Entity／Schema 關係為 `1:N`，但 V1 service flow 以重複 Review 檢查限制每筆申請只能完成一次審核。Drawdown 對 CreditLimit 與 AppUser（`createdBy`）皆使用 LAZY `@ManyToOne`。

User 參照：

- `CreditApplication.createdBy`（已完成）
- `CreditReview.reviewedBy`（Planned；目前 reviewer 身分只保存於 AuditLog 的 actor username snapshot）
- `Drawdown.createdBy`（已完成）
- `AuditLog.actorUsername`（已完成；保存 SecurityContext username snapshot）

目前已完成 CreditApplication／Drawdown 的 `createdBy` ownership、主要關聯的 LAZY fetch、Database identifier、必要 Foreign Key／UNIQUE constraint／index、Drawdown pessimistic locking，以及以 `NUMERIC(19, 2)`／`BigDecimal` 表示金額。未實作項目會在本文明確標示為 Planned 或 Optional。

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

- 建立申請時 `requestedAmount` 不可為 null，且必須大於零；Stage 9 已由 Service 驗證並補上 automated tests。
- 修改自己擁有且為 DRAFT 的授信申請仍為 Planned；目前沒有 update API。
- 只有 DRAFT 的授信申請可以 Submit。
- 只有 SUBMITTED 的授信申請可以 Approve 或 Reject。

### CreditReview

- 只有 REVIEWER 可以審核授信申請。
- Maker-Checker／Segregation of Duties 是 Service 層的 Business Rule：即使 Role Authorization 允許 Review，reviewer 也不得與 `CreditApplication.createdBy` 是同一位 User。
- Endpoint Authorization 與 RBAC 檢查不能取代 reviewer 與建立者的身分驗證。
- 執行 Approve 時，`approvedAmount` 必須大於零。
- `approvedAmount` 不得大於 `requestedAmount`。
- Reject 必須填寫原因。

Stage 4 已完成 `SUBMITTED` 狀態檢查、重複 Review 防護、`approvedAmount > 0`、`approvedAmount <= requestedAmount`，以及 Reject comment 不得為 null／blank；Stage 5 已完成 REVIEWER Authorization 與 Maker-Checker；Stage 9 已補齊建立申請時的 `requestedAmount` 驗證。

### CreditLimit

- 只有 APPROVED 的 CreditApplication 才能建立 CreditLimit。
- 一個 CreditApplication 最多只能有一個 CreditLimit。
- 每筆 Drawdown 不得超過目前 `availableAmount`；扣減後 `availableAmount` 不得小於零。

Stage 4 已完成由 Approve 流程建立唯一 CreditLimit，並在建立時令 `availableAmount = limitAmount`；Stage 6 已完成 Drawdown 與 `availableAmount` 扣減規則。

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

Stage 7 已將 Approve／Reject 的 AuditLog 納入同一個既有 Transaction Boundary；business action 或 Audit write 失敗時一起 rollback。

### Drawdown

- `amount` 不得為 null 且必須大於零，否則回傳 400；超過 `availableAmount` 屬於 Business Conflict，回傳 409。
- CreditLimit 不存在時回傳 404。
- `POST /api/credit-limits/{creditLimitId}/drawdowns` 僅允許 RM，並從 JWT SecurityContext 取得目前使用者作為 `createdBy`。
- `DrawdownService.create()` 使用 `@Transactional`，以 `PESSIMISTIC_WRITE` 鎖定 CreditLimit，再建立 Drawdown 並扣減 `availableAmount`。
- CreditLimit 是 managed Entity，額度扣減透過 JPA Dirty Checking 寫回；任一步驟失敗時整筆交易 rollback。
- Stage 7 已將 `CREATE_DRAWDOWN` AuditLog 納入既有 Transaction Boundary；超額失敗時 availableAmount 不變且不產生成功 Audit。

## 7. Audit Strategy

Stage 7 已完成的 Audit Actions：

- `CREATE_CREDIT_APPLICATION`
- `SUBMIT_CREDIT_APPLICATION`
- `APPROVE_CREDIT_APPLICATION`
- `REJECT_CREDIT_APPLICATION`
- `CREATE_DRAWDOWN`

AuditLog 包含：

- `actorUsername`
- `action`
- `entityType`
- `entityId`
- `createdAt`

`action` 與 `entityType` 分別使用 `AuditAction`／`AuditEntityType` enum。Actor 由 JWT 建立的 SecurityContext 取得，只保存 username，不保存 JWT、密碼或 Authorization credential。`AuditLogService.record()` 使用 `Propagation.MANDATORY`，強制 Audit write 加入既有 business transaction。

Application Log 是營運／診斷用途的 Log。AuditLog 則是記錄何人在何時對哪個 Target 執行相關操作的持久性業務紀錄。兩者是不同概念，不得視為可互換的資訊。

## 8. Architecture

規劃採用 Layered Architecture：

`Controller → Service → Repository → PostgreSQL`

並搭配 DTO、Entity、Validation、Exception 與 Security 等關注面向。V1 維持 Modular Monolith；Microservices 不在範圍內。

### Coding Rules

1. Controller 不得包含核心 Business Logic。
2. Controller 不得直接呼叫 Repository。
3. API Request 使用 DTO；CreditApplication 與 Drawdown 已使用 Response DTO 隔離 API Contract 與 Entity，其他 API 將依 Stage 持續完善。
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
- springdoc-openapi／Swagger UI
- PostgreSQL JDBC Driver
- Flyway Core 與 Flyway PostgreSQL Support
- Spring Boot Test Dependency
- Spring Security Test Dependency

JWT Authentication 已於 Stage 5 完成；Stage 8 已使用 JUnit 5、Mockito 與 Spring Security Test 補強自動化測試，並設定 GitHub Actions CI；Stage 10 已完成 springdoc-openapi、Swagger UI 與 JWT Bearer authorization。Docker／Deployment 保留至 Optional Stage 11。Testcontainers 為尚未實作的高優先選配加分項。Spring Batch 或 Scheduling 僅在後續有合理用途且時程允許時採用。

Lombok 不應大量依賴。Redis、Kafka、Kubernetes、Elasticsearch 與 Microservices 排除於 V1 範圍外。

### API Map

目前已實作 Login、public Company APIs、CreditApplication 的建立／Submit／Approve／Reject／分頁查詢，以及 Drawdown API；CreditApplication／Drawdown 端點已套用 JWT 與 RBAC，審核另有 Maker-Checker。Audit write 已完成，AuditLog 查詢 API 仍為 Planned。

| 功能 | Method／Path | 主要 Role | 狀態 |
| --- | --- | --- | --- |
| Login | `POST /api/auth/login` | Anonymous | 已實作；成功簽發 JWT |
| 建立 Company | `POST /api/companies` | Public（目前）／RM（目標） | 已實作；V1 目前為 public，正式 RBAC 未完成 |
| 查詢 Company | `GET /api/companies`, `GET /api/companies/{id}` | Public（目前）／內部 User（目標） | 已實作；V1 目前為 public，正式 RBAC 未完成 |
| 建立 CreditApplication | `POST /api/credit-applications` | RM | 已實作；初始狀態固定 `DRAFT`；自動記錄 `createdBy` |
| 更新自己擁有的 DRAFT | `PUT /api/credit-applications/{id}` | RM | 規劃 |
| Submit 授信申請 | `POST /api/credit-applications/{id}/submit` | RM | 已實作；僅允許 `DRAFT → SUBMITTED`；RBAC 已完成 |
| 查詢授信申請 | `GET /api/credit-applications` | Authenticated User | 已實作；支援 status、page、size、sort |
| Approve 授信申請 | `POST /api/credit-applications/{id}/approve` | REVIEWER | 已實作；RBAC／Maker-Checker 已完成 |
| Reject 授信申請 | `POST /api/credit-applications/{id}/reject` | REVIEWER | 已實作；RBAC／Maker-Checker 已完成 |
| 查看 CreditReview | `GET /api/credit-applications/{id}/reviews` | REVIEWER | 規劃 |
| 查看 CreditLimit | `GET /api/credit-applications/{id}/credit-limit` | REVIEWER 與已授權的業務 User | 規劃 |
| 建立 Drawdown | `POST /api/credit-limits/{creditLimitId}/drawdowns` | RM | 已實作；RBAC、交易與並行額度控制已完成 |
| 管理 User／Role | `/api/admin/users`, `/api/admin/roles` | ADMIN | 規劃 |
| 查詢 AuditLog | `GET /api/admin/audit-logs` | ADMIN | 規劃 |

Company Create Request DTO 與基本 Validation 已完成。CreditApplication 與 Drawdown 已使用 Request／Response DTO，避免直接序列化 Hibernate LAZY Proxy。Stage 7 已以 `@RestControllerAdvice`／`@ExceptionHandler` 與 `ApiErrorResponse` 統一 400／404／409 response；未預期錯誤維持 500 且不暴露內部細節。CreditApplication 查詢已支援 status filtering、DB pagination、sorting、page／size validation 與最大 size 100。

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
- `V6__create_drawdowns_table.sql` Migration。
- `V7__create_audit_logs_table.sql` Migration。
- `company`、`app_user`、`credit_applications`、`credit_reviews`、`credit_limits`、`drawdowns`、`audit_logs` 與 `flyway_schema_history`。
- `credit_applications.company_id` Foreign Key 參照 `company(id)`。
- `credit_applications.created_by` Foreign Key 參照 `app_user(id)`。
- `credit_reviews.application_id` Foreign Key 參照 `credit_applications(id)`，且不設 UNIQUE，以支援 CreditApplication `1:N` CreditReview。
- `credit_limits.application_id` Foreign Key 參照 `credit_applications(id)`，並以 UNIQUE constraint 保證 CreditApplication `1:0..1` CreditLimit。
- `drawdowns.credit_limit_id` 與 `drawdowns.created_by` 分別參照 `credit_limits(id)` 與 `app_user(id)`。
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
- Company API 目前明確維持 `permitAll`，屬於 V1 limitation，尚未套用目標 RM／內部 User RBAC。
- Method-level Security 以 `@PreAuthorize` 限制 RM 建立／Submit、REVIEWER Approve／Reject；ADMIN 不自動取得授信流程權限。
- Drawdown 建立僅允許 RM；REVIEWER 呼叫時回傳 403，建立者由 JWT SecurityContext 取得。
- Service 以 `CreditApplication.createdBy` 比對目前登入者，禁止 maker Approve／Reject 自己的案件。
- `manual-test` profile 使用 CommandLineRunner 初始化 BCrypt 測試帳號，不以測試 seed 污染 Flyway migration history。
- Swagger UI 與 OpenAPI JSON 路徑已設為 `permitAll`；既有 Business API Security 與 `@PreAuthorize` 規則維持不變。

## 11. Testing Strategy

### Current Status

- 已使用 JUnit 5／Mockito 驗證 CreditApplication、Drawdown 與 Audit 核心 Business Rules，並使用 Spring Security Test 驗證實際 Security Filter Chain 與 Method Security。
- Stage 8 baseline 為 59 tests；Stage 9 加入 3 個 `requestedAmount` 參數化案例後，目前共 62 tests，0 failures／0 errors／0 skipped，`BUILD SUCCESS`。
- VS Code REST Client 人工 Company API 驗證已完成。
- VS Code REST Client 已人工驗證 CreditApplication 建立與 Submit 成功。
- PostgreSQL 已人工確認 CreditApplication 資料寫入，以及 `DRAFT → SUBMITTED` 狀態轉換。
- 重複 Submit 已由 Business Rule 阻擋；Stage 7 全域錯誤處理回傳 409 Conflict。
- Stage 4 REST Client 與 PostgreSQL 人工驗證已完成：Application #4 由 requestedAmount 9,000,000 核准 6,000,000，最終狀態為 `APPROVED`，CreditReview decision 為 `APPROVED`，CreditLimit 的 limitAmount 與 availableAmount 均為 6,000,000。
- Reject 人工驗證已完成：Application #6 的 requestedAmount 為 5,000,000，最終狀態為 `REJECTED`，CreditReview decision 為 `REJECTED`、approvedAmount 為 NULL，且未建立 CreditLimit。
- DRAFT 直接 Approve、approvedAmount 大於 requestedAmount、Reject comment 空白，以及已 APPROVED 再次 Approve 均已人工確認被阻擋。
- Stage 4 原有 Business Rule 錯誤已於 Stage 7 納入一致的 400／404／409 error contract。
- Stage 5 已以 manual-test profile 與 VS Code REST Client 人工驗證 Login、JWT、RM／REVIEWER RBAC 與 Maker-Checker。
- Stage 6 已人工驗證 RM 正常建立 Drawdown（201）、amount = 0（400）、超額（409 Conflict），以及 REVIEWER 建立 Drawdown（403）。
- `DrawdownServiceTest` 已驗證 CreditLimit 不存在、null／0／負數／超額金額，以及 amount 等於 availableAmount 時成功且剩餘額度為 0。
- Stage 7 automated tests 共 29 個，涵蓋 Global Exception Handling、Audit、Transaction Boundary、CreditApplication filtering／pagination 與 Drawdown business conflict；`mvnw test` 為 0 failures／0 errors。
- Stage 8 補強 CreditApplication Maker-Checker、Approve／Reject invalid status、invalid approvedAmount、duplicate Review／CreditLimit 與 invalid reject reason Unit Tests。
- Stage 8 Security Tests 經實際 Security Filter Chain 與 `@PreAuthorize` 驗證 anonymous request 回傳 401、已登入但角色不足回傳 403，以及 RM／REVIEWER／ADMIN RBAC；測試曾發現 anonymous request 原本回傳 403，SecurityConfig 加入 AuthenticationEntryPoint 後修正為 401。Invalid／expired JWT 的 automated integration test 尚未實作。
- Stage 8 完成時累計 59 tests；Stage 9 補上 `requestedAmount` null／0／負數案例後累計 62 tests。
- GitHub Actions workflow 已設定 push／pull request、Ubuntu、JDK 21、Maven Wrapper、dependency cache、`test` 與 `package`；Stage 8 commit `8d355b9` 的 remote CI 已完成並通過。
- Stage 7 Manual Test 已驗證 pagination、filtering、sorting、400／404／409、Approve／Reject／Drawdown Audit Trail、失敗 action 不產生成功 Audit，以及超額 Drawdown 後 availableAmount 不變。
- `mvnw package` 為 `BUILD SUCCESS`，`git diff --check` PASS。
- Stage 9 Manual Final Verification 已完成 22 項 REST 驗證：Health／Company／Login、anonymous 與 invalid JWT 401、`requestedAmount` 400、CreditApplication Create／Submit／Approve／Reject、RBAC 403、Business Conflict 409、Filtering／Pagination／Sorting，以及 Drawdown 201／400／403／409 均符合目前 API contract。
- Stage 9 PostgreSQL 驗證已確認 Application 16 正確建立 CREATE／SUBMIT／APPROVE AuditLog、Application 17 正確建立 CREATE／SUBMIT／REJECT AuditLog，CreditLimit 5 由 3,000,000 成功扣減為 2,900,000，Drawdown 3 正確建立，且後續失敗操作不會再次扣減額度或新增成功 AuditLog。上述 ID 僅為本次驗證紀錄，不作為永久測試資料依賴。
- Stage 10 已人工驗證 Swagger UI、OpenAPI JSON、JWT Bearer authorization 與主要 API 顯示；CreditApplication 查詢的 `sort` 已呈現為單一 query string，`createdAt,desc` 與省略 sort 均可正常呼叫。

### Unit Test

- 使用 JUnit 5 與 Mockito。
- 主要聚焦於 Service Business Rules 與 State Transition。

### Integration Test

- Testcontainers／Database Integration Tests 尚未實作。
- 真實資料庫 API、Repository 與 Transaction Rollback 驗證保留為後續加分項。

### Security Test

Stage 8 已驗證：

- 測試實際經過 Security Filter Chain 與 Method Security／`@PreAuthorize`。
- 未登入使用者呼叫受保護 API 回傳 401；已登入但角色不足回傳 403。
- RM／REVIEWER／ADMIN 的 CreditApplication 與 Drawdown 基本 RBAC 規則。

Invalid JWT 回傳 401 已納入 `api-test.http` 人工驗證流程；invalid／expired JWT 的 automated integration test 為尚未實作項目，不列為已驗證的 automated coverage。

### Transaction Test

Stage 4 現階段 Approve 任一步驟失敗時，驗證以下項目必須一併 rollback：

- CreditApplication 不得錯誤地停留在 APPROVED。
- CreditReview 不得只完成部分 insert。
- CreditLimit 不得只完成部分 insert。

Stage 4 現階段 Reject 任一步驟失敗時，驗證以下項目：

- CreditApplication 不得錯誤地停留在 REJECTED。
- CreditReview 不得只完成部分 insert。

Stage 7 已以 `Propagation.MANDATORY` 強制 AuditLog 加入 Approve／Reject 的既有 transaction，並以 automated test 驗證所有 audited actions 都具備 transaction boundary。Stage 8 未實作真實資料庫 rollback integration test。

Drawdown 失敗時，驗證以下所有項目：

- CreditLimit 不得被錯誤更新。
- Drawdown 不得 insert。
- AuditLog 不得 insert。

Testcontainers 是高優先加分項，但若時程壓力需要可省略。本專案不追求 100% Coverage；目標是充分涵蓋具風險的規則與 Boundary。

## 12. Delivery／CI 與 Optional Deployment

- 前期開發：Local Java 加 Local PostgreSQL。
- 核心交付：可執行 JAR、文件、測試與最終驗證。
- CI Platform：GitHub Actions。
- `.github/workflows/ci.yml` 已設定 push／pull request trigger、Ubuntu、JDK 21、Maven dependency cache，以及 `./mvnw test`／`./mvnw package`。
- CI workflow implementation 已完成；Stage 8 remote CI 已成功執行 `test` 與 `package`。
- Docker／Docker Compose 與 Public Deployment 保留至 Optional Stage 11。

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
| Stage 6 | Drawdown／Transaction | Completed | 2026-09-08 |
| Stage 7 | Audit／Exception／Filtering／Pagination | Completed | 2026-09-09 |
| Stage 8 | Automated Tests／CI | Completed；Local／Remote CI Verified | 2026-09-10 |
| Stage 9 | Documentation／Final Verification | Completed | 2026-09-10 |
| Stage 10 | OpenAPI／Swagger UI | Completed | 2026-09-10 |
| Stage 11 | Docker／Docker Compose／Public Deployment | Optional | - |

Stage 0～10 已完成；Stage 11 為 Optional，不得排擠 Business Logic、Security、Testing 等核心工作。

### Stage 10 — OpenAPI／Swagger UI（Completed）

- 加入 `springdoc-openapi-starter-webmvc-ui`。
- 新增 OpenAPI config，設定 API 基本資訊與 JWT Bearer security scheme。
- 提供 Swagger UI 與 `/v3/api-docs` OpenAPI JSON。
- SecurityConfig 僅放行 Swagger UI／OpenAPI 必要路徑，既有 Business API Security 不變。
- 主要 Controller 加入 `@Tag`，受保護 Controller 加入 `@SecurityRequirement`。
- 修正 CreditApplication 查詢的 `sort` 文件，Swagger UI 以 `createdAt,desc` 或 `id,desc` query string 操作。
- 已人工驗證 Swagger UI 顯示、JWT Authorize、受保護 API 與 sorting，結果 PASS。

### Stage 11 — Docker／Deployment（Optional）

- 規劃 Spring Boot Dockerfile 與 Docker Compose one-command startup。
- 以 Environment Variables 提供資料庫連線與必要設定，啟動時由 Flyway 執行 Migration。
- 將完成的核心系統部署至 Public Environment。
- 僅在 Stage 0～10 核心工作穩定完成後進行。
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

Stage 10 OpenAPI／Swagger UI 已於 2026-09-10 完成；Stage 11 Docker／Deployment 為 Optional，不設定核心交付期限。

2026-09-08 後不得新增大型功能。

## 14. Current Status

**Current Status：Stage 0～10 核心開發與驗證已完成**

**Next Stage：Stage 11 Docker／Docker Compose／Deployment（Optional）**

- AppUser／Role 與對應 Repository、Flyway V4／V5 Migration 已完成；CreditApplication 會保存建立者 `createdBy`。
- BCrypt PasswordEncoder、UserDetailsService、AuthenticationManager 與 Login API 已完成。
- JWT 產生／驗證、JWT Authentication Filter、SecurityContext 建立與 Stateless Session 已完成。
- Method-level RBAC 已限制 RM 建立／Submit，以及 REVIEWER Approve／Reject；ADMIN 不作為授信流程 super user。
- Maker-Checker 已在 Service 層阻擋建立者 Approve／Reject 自己的案件。
- `manual-test` profile 以 CommandLineRunner 建立本機 BCrypt 測試帳號，並已完成人工 Security 整合驗證。
- Drawdown 已完成 RM-only RBAC、JWT current user／`createdBy`、交易邊界、Pessimistic Lock、`availableAmount` 扣減與 Dirty Checking。
- Drawdown 的 CreditLimit 不存在回傳 404，null／0／負數回傳 400，超額 availableAmount 回傳 409；REVIEWER 建立回傳 403。
- Stage 6 人工測試 PASS；Stage 8 已補 Drawdown 全額動用邊界測試，驗證 availableAmount 歸零。
- Stage 7 已完成 `@RestControllerAdvice`／`@ExceptionHandler`、統一 `ApiErrorResponse`，以及 400／404／409 error mapping。
- AuditLog 已以 enum action／entity type、SecurityContext actor 與 `Propagation.MANDATORY` 納入五項核心 business transactions。
- CreditApplication list API 已完成 status filtering、DB pagination、sorting、page／size validation 與 Response DTO。
- Stage 7 automated tests baseline 為 29 個；Stage 8 補強 CreditApplication Business Rules、Maker-Checker、Security RBAC 與 Drawdown 邊界後累計 59 tests；Stage 9 requestedAmount 驗證案例加入後累計 62 tests。
- 目前本機 `mvnw test` 為 0 failures／0 errors／0 skipped，`BUILD SUCCESS`；Stage 8 GitHub Actions remote CI 亦已通過。
- Testcontainers、DB integration tests、invalid／expired JWT automated integration test 與 concurrency integration tests 尚未實作。
- Stage 7 Manual Test 已驗證查詢、錯誤 response、Audit Trail、失敗 action 不留成功 Audit，以及超額 Drawdown rollback 行為。
- Stage 9 Manual Final Verification 的 22 項 REST 結果均符合預期；PostgreSQL 已確認 Application 16／17、CreditLimit 5、Drawdown 3 的狀態、金額與 AuditLog，失敗操作沒有額外扣減或成功 Audit。
- Stage 8 implementation／local／remote CI verification 與 Stage 9 documentation／manual final verification 均已於 2026-09-10 完成。
- Stage 10 已完成 springdoc-openapi、Swagger UI、OpenAPI config、JWT Bearer authorization、Swagger Security permit paths、Controller 文件標註與 `sort` 參數修正，並通過人工驗證。

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

Stage Tags 為 `v1-stage-0` 至 `v1-stage-10`。最終 Release Tag 為 `v1.0.0`。

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
