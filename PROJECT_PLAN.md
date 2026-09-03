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

目前僅 Company 已完成實作，其餘項目仍屬後續 Stage。

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

User 與 Role 要採用單一 Role 或多 Role 關係，仍是留待 Security／RBAC Stage 決定的明確設計議題。Maker-Checker 規則在任一模型下都必須成立。

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
- CreditApplication `1:N` CreditReview
- CreditApplication `1:0..1` CreditLimit
- CreditLimit `1:N` Drawdown

規劃中的 User 參照：

- `CreditApplication.createdBy`
- `CreditReview.reviewedBy`
- `Drawdown.createdBy`
- `AuditLog.user`

精確的 ownership、fetch、cascade、identifier、indexing、locking 與金額欄位設計，將在排定的 Stage 中進行決策與 Review，不預先假設。

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

### CreditLimit

- 只有 APPROVED 的 CreditApplication 才能建立 CreditLimit。
- 一個 CreditApplication 最多只能有一個 CreditLimit。
- `usedAmount` 不得大於 `approvedAmount`。

### Approve 與 Reject 的 Transaction Boundary

成功執行 Approve 時，以下所有操作必須位於同一個 Transaction：

1. 將 CreditApplication 從 `SUBMITTED` 轉換為 `APPROVED`。
2. 建立 CreditReview。
3. 建立該授信申請唯一的 CreditLimit。
4. 建立 AuditLog。

任何步驟失敗時，整個 Approve Transaction 都必須 rollback。不得留下只完成部分更新的授信申請、CreditReview、CreditLimit 或 AuditLog。

Reject 操作必須在同一個一致的 Transaction Boundary 中執行以下所有操作：

1. 將 CreditApplication 從 `SUBMITTED` 轉換為 `REJECTED`。
2. 建立 CreditReview。
3. 建立 AuditLog。

任何 Reject 步驟失敗時，Transaction 必須 rollback，且不得留下部分成功的資料。

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
3. API Request 使用 DTO；Response DTO 與 API Contract 隔離仍是後續要完善的設計原則。
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
- Spring Security Dependency
- Spring Validation
- PostgreSQL JDBC Driver
- Flyway Core 與 Flyway PostgreSQL Support
- Spring Boot Test Dependency

後續規劃使用 JWT Authentication、OpenAPI／Swagger、JUnit 5、Mockito、Docker Compose 與 GitHub Actions。Testcontainers 為高優先選配加分項。Spring Batch 或 Scheduling 僅在後續有合理用途且時程允許時採用。

Lombok 不應大量依賴。Redis、Kafka、Kubernetes、Elasticsearch 與 Microservices 排除於 V1 範圍外。

### API Map

目前僅 Company APIs 已實作，其餘為規劃層級，並非目前已完成的 API Contract。

| 功能 | Method／Path | 主要 Role | 狀態 |
| --- | --- | --- | --- |
| 建立 Company | `POST /api/companies` | RM | 已實作；正式 RBAC 未完成 |
| 查詢 Company | `GET /api/companies`, `GET /api/companies/{id}` | 已授權的內部 User | 已實作；正式 RBAC 未完成 |
| 建立 CreditApplication | `POST /api/credit-applications` | RM | 規劃 |
| 更新自己擁有的 DRAFT | `PUT /api/credit-applications/{id}` | RM | 規劃 |
| Submit 授信申請 | `POST /api/credit-applications/{id}/submit` | RM | 規劃 |
| 查詢授信申請 | `GET /api/credit-applications` | RM, REVIEWER | 規劃 |
| Approve 授信申請 | `POST /api/credit-applications/{id}/approve` | REVIEWER | 規劃 |
| Reject 授信申請 | `POST /api/credit-applications/{id}/reject` | REVIEWER | 規劃 |
| 查看 CreditReview | `GET /api/credit-applications/{id}/reviews` | REVIEWER | 規劃 |
| 查看 CreditLimit | `GET /api/credit-applications/{id}/credit-limit` | REVIEWER 與已授權的業務 User | 規劃 |
| 建立 Drawdown | `POST /api/credit-limits/{id}/drawdowns` | RM | 規劃 |
| 管理 User／Role | `/api/admin/users`, `/api/admin/roles` | ADMIN | 規劃 |
| 查詢 AuditLog | `GET /api/admin/audit-logs` | ADMIN | 規劃 |

Company Create Request DTO 與基本 Validation 已完成。通用 Response DTO、Response Code、Filtering、Pagination、Idempotency、Concurrency Control 與 Error Contract，仍將在各功能設計時定義。

## 9. Database Strategy

目前已完成：

- PostgreSQL Database 與 Spring Boot DataSource Connection。
- Spring Data JPA、Jakarta Persistence／JPA 與 Hibernate Entity Mapping。
- Flyway Core 與 Flyway PostgreSQL Support。
- `V1__create_company_table.sql` Migration。
- `company` 與 `flyway_schema_history`。
- `spring.jpa.hibernate.ddl-auto=validate`。

原則：

- Flyway 負責建立與修改 Database Schema。
- Hibernate 負責 Object-Relational Mapping、Persistence Operations 與 Schema Validation。
- 正式 Schema 不由 Hibernate 自動建立。
- `ddl-auto=update` 不得作為正式的 Schema Management Strategy。
- Database Connection Details 必須外部化，Secret 不得 commit。

## 10. Security Strategy

### Planned

- Authentication
- JWT
- RBAC
- RM、REVIEWER、ADMIN 權限邊界
- Maker-Checker／Segregation of Duties

### Current

- 僅有 Development `SecurityConfig`，不是正式 Security 機制。
- Health、Company API 與 `/error` 暫時設為 `permitAll`。
- CSRF 暫時關閉。
- 正式 Authentication、JWT 與 RBAC 尚未完成。

## 11. Testing Strategy

### Current Status

- 尚未建立正式 Automated Test Classes。
- Maven `test` 與 `package` Lifecycle 均為 `BUILD SUCCESS`。
- VS Code REST Client 人工 Company API 驗證已完成。
- PostgreSQL Persistence 寫入驗證已完成。

### Unit Test

- 使用 JUnit 5 與 Mockito。
- 主要聚焦於 Service Business Rules 與 State Transition。

### Integration Test

- 使用 Spring Boot Test。
- 涵蓋核心 API、Repository 與 Database Flow。

### Security Test

至少驗證：

- 未登入的 Request 回傳 401。
- RM 呼叫 Approve 回傳 403。
- REVIEWER 合法執行 Approve 時成功。

### Transaction Test

Approve 任一步驟失敗時，驗證以下項目必須一併 rollback：

- CreditApplication 不得錯誤地停留在 APPROVED。
- CreditReview 不得只完成部分 insert。
- CreditLimit 不得只完成部分 insert。
- AuditLog 不得只完成部分 insert。

Reject 任一步驟失敗時，驗證 CreditApplication、CreditReview 與 AuditLog 仍保持一致，且沒有部分結果被 commit。

Drawdown 失敗時，驗證以下所有項目：

- CreditLimit 不得被錯誤更新。
- Drawdown 不得 insert。
- AuditLog 不得 insert。

Testcontainers 是高優先加分項，但若時程壓力需要可省略。本專案不追求 100% Coverage；目標是充分涵蓋具風險的規則與 Boundary。

## 12. Delivery／Docker／CI

- 前期開發：Local Java 加 Local PostgreSQL。
- 後期 Packaging：以 Docker Compose 執行 Spring Boot 與 PostgreSQL。
- CI Platform：GitHub Actions。
- 最低 CI Commands：`mvn test` 與 `mvn package`。
- Public Deployment 為選配。

Deployment 工作不得排擠 Business Logic、Transaction 正確性、Security 或 Testing。

## 13. Development Stages

| Stage | Scope | Status | Actual Completion |
| --- | --- | --- | --- |
| Stage 0 | Project Planning／Initialization | Completed | 2026-08-22 |
| Stage 1 | Spring Boot Skeleton | Completed | 2026-08-25 |
| Stage 2 | PostgreSQL／JPA／Flyway／Company API | Completed | 2026-09-03 |
| Stage 3 | CreditApplication／Submit | Next | - |
| Stage 4 | CreditReview／Approve／Reject／CreditLimit | Planned | - |
| Stage 5 | Security／JWT／RBAC／Maker-Checker | Planned | - |
| Stage 6 | Drawdown／Transaction | Planned | - |
| Stage 7 | Audit／Exception／Filtering／Pagination | Planned | - |
| Stage 8 | Automated Tests／CI | Planned | - |
| Stage 9 | Docker／Documentation／Final Verification | Planned | - |

### Original Schedule

原始規劃日期皆為 2026 年，時區為 Asia/Taipei；每個開發日預計投入約 5～6 小時，目標完成日維持 2026-09-10。

- Stage 0：8/21–8/22
- Stage 1：8/23–8/25
- Stage 2：原訂 8/26，實際完成日為 9/3
- Stage 3：原訂 8/27
- Stage 4：原訂 8/28–8/29
- Stage 5：原訂 8/30–8/31
- 9/1–9/2：不安排電腦開發
- Stage 6：原訂 9/3–9/4
- Stage 7：原訂 9/5
- Stage 8：原訂 9/6–9/7
- Stage 9：原訂 9/8–9/10

2026-09-08 後不得新增大型功能。

## 14. Current Status

**Stage 2 Completed**

- PostgreSQL DataSource、JPA／Hibernate 與 Flyway V1 已完成。
- Company Entity、Repository、Service、Request DTO、Validation、Controller 與三個 API 已完成。
- REST Client 與 PostgreSQL 寫入人工驗證已完成。
- Maven `test`／`package` Lifecycle 均為 `BUILD SUCCESS`，但尚無正式 Automated Test Classes。
- Stage 2 已於 2026-09-03 推送至 GitHub，Tag 為 `v1-stage-2`。
- Stage 3 尚未開始。

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

Codex 不得代替使用者完整撰寫核心實作。

- 使用者負責理解與實作核心 Business Logic。
- 第一次接觸核心技術時，由使用者先學習並撰寫第一版。
- Codex 主要協助 Review、Boilerplate、重複工作、Automation、Documentation 與 Git 操作。
- 未經明確同意，Codex 不得導入大型 Framework 或改變 Architecture。
- 修改核心 Code 前，Codex 必須說明修改目的。
- 修改後，Codex 必須列出變更檔案與每項變更原因。
- 使用者必須 Review 所有核心 Diff。

以下功能第一次實作時，Codex 不得從零建立完整功能：

- JPA Relationship
- Transaction
- Spring Security
- Business Rules
- Exception Handling
- Integration Test

即使 Code Generation 速度較快，這些規則仍要求 Assistant 在適當的學習／Review Boundary 暫停。
