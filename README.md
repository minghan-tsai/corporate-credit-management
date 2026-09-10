# Corporate Credit Management System

以 Java 21 與 Spring Boot 開發的企業授信管理 Backend Portfolio Project，模擬銀行內部從企業建檔、授信申請、獨立審核、建立額度到 Drawdown 的核心流程。

本專案刻意採用「小而深」的範圍，重點放在可說明的 Business Rules、Transaction 一致性、JWT／RBAC、Maker-Checker、`PESSIMISTIC_WRITE` 悲觀鎖與額度一致性控制，以及 Audit Trail，而不是重現完整 Core Banking System。

## 核心流程

`Company → CreditApplication → Submit → Approve / Reject → CreditLimit → Drawdown`

合法的授信申請狀態轉換：

- `DRAFT → SUBMITTED → APPROVED`
- `DRAFT → SUBMITTED → REJECTED`

## 目前進度

| Stage | 範圍 | 狀態 |
| --- | --- | --- |
| Stage 0～7 | 專案骨架、授信流程、Security、Drawdown、Audit、Exception、Pagination | Completed |
| Stage 8 | Automated Tests／CI | Completed；Local 與 GitHub Actions 均已驗證 |
| Stage 9 | Documentation／Final Verification | Completed |
| Stage 10 | Docker／Docker Compose | Optional |
| Stage 11 | Public Deployment | Optional |

Stage 8 的 59 tests 已在本機及 GitHub Actions 通過。Stage 9 已加入 `requestedAmount` 建立驗證的 automated tests，目前完整測試數為 62；本機 `mvnw test` 與 `mvnw package` 均為 `BUILD SUCCESS`。

Stage 9 Manual Final Verification 已完成：Health、Company、RM／REVIEWER Login、401／403 Security Boundary、`requestedAmount` 400、CreditApplication Create／Submit／Approve／Reject、409 Business Conflict、Filtering／Pagination／Sorting，以及 Drawdown 201／400／403／409 均符合目前 API contract。PostgreSQL 亦已確認 Approve／Reject／CreditLimit／Drawdown 寫入結果、`availableAmount` 由 3,000,000 正確扣減為 2,900,000，且失敗操作不會再次扣減額度或留下成功 AuditLog。

## 已實作功能與規則

### Company

- 建立公司、查詢單一公司、查詢全部公司。
- `name` 不可為空白。
- `taxId` 不可為空白、長度必須為 8，並由 Database UNIQUE constraint 保證唯一。
- **V1 limitation：Company API 目前為 public，尚未套用正式 RBAC。**

### CreditApplication

- 只有 RM 可以建立與 Submit 授信申請。
- 建立者由 JWT SecurityContext 取得並保存為 `createdBy`。
- 新申請初始狀態固定為 `DRAFT`。
- `requestedAmount` 不可為 null，且必須大於 0。
- 只有 `DRAFT` 可以 Submit；重複 Submit 回傳 409 Conflict。
- 查詢 API 支援 status filtering、database pagination 與 Spring Data sorting。
- Pagination 預設為 `page=0`、`size=20`、`sort=id,desc`，最大 `size` 為 100。

### CreditReview／CreditLimit

- 只有 REVIEWER 可以 Approve 或 Reject。
- Maker-Checker 在 Service 層禁止建立者審核自己的案件。
- 只有 `SUBMITTED` 可以 Approve 或 Reject。
- V1 service flow 僅允許一次完成審核。
- `approvedAmount` 不可為 null、必須大於 0，且不得超過 `requestedAmount`。
- Reject comment 不可為 null 或空白。
- Approve 建立 CreditReview 與唯一 CreditLimit；Reject 只建立 CreditReview。
- CreditLimit 建立時 `availableAmount = limitAmount`。
- Approve／Reject 使用 `@Transactional`，狀態、Review、Limit 與 Audit 必須一起 commit／rollback。

### Drawdown

- 只有 RM 可以建立 Drawdown，建立者由 JWT SecurityContext 取得。
- Amount 不可為 null、0 或負數。
- Amount 不得超過 `availableAmount`；等於可用額度時允許動用並歸零。
- 使用 `PESSIMISTIC_WRITE` 鎖定 CreditLimit，避免同一額度並行超額動用。
- Drawdown、`availableAmount` 扣減與 Audit 共用同一 Transaction。

### Audit 與錯誤處理

- AuditLog 記錄 actor username、action、entity type／ID 與時間。
- 已記錄五項 action：建立申請、Submit、Approve、Reject、建立 Drawdown。
- `AuditLogService.record()` 使用 `Propagation.MANDATORY` 加入既有 business transaction。
- **AuditLog 目前只有寫入，尚未提供查詢 API。**
- `ApiErrorResponse` 統一處理常見 400／404／409 錯誤，並安全處理未預期的 500 response。
- Authentication 與 Authorization 分別使用 401／403。

## 技術棧

- Java 21
- Spring Boot 3.5.16
- Maven Wrapper
- Spring Web／Validation
- Spring Data JPA／Hibernate
- PostgreSQL
- Flyway
- Spring Security／BCrypt／JWT（JJWT）
- JUnit 5／Mockito／Spring Security Test
- GitHub Actions

## Prerequisites

開始前需準備：

- JDK 21
- PostgreSQL
- 可使用 PowerShell、Command Prompt 或支援 shell script 的終端機
- 不需要另外安裝 Maven；專案已包含 Maven Wrapper
- 若要執行 `api-test.http`，建議安裝 VS Code REST Client extension

## PostgreSQL 基本設定

預設連線設定：

- Host：`localhost`
- Port：`5432`
- Database：`corporate_credit_management`
- Username：預設 `postgres`，可由 `DB_USERNAME` 覆寫
- Password：必須由 `DB_PASSWORD` 提供

先建立 database：

```sql
CREATE DATABASE corporate_credit_management;
```

應用程式啟動時會由 Flyway 依序執行 V1～V7 migration；Hibernate 使用 `ddl-auto=validate`，不會自行建立正式 schema。

## Environment Variables

| 變數 | 必要性 | 說明 |
| --- | --- | --- |
| `DB_PASSWORD` | 必要 | PostgreSQL password |
| `DB_USERNAME` | 選填 | 預設為 `postgres` |
| `SPRING_DATASOURCE_URL` | 選填 | 覆寫預設 PostgreSQL URL |
| `JWT_SECRET` | Production 必要 | Base64 編碼的 JWT signing key；未設定時只會使用 local development default |
| `JWT_EXPIRATION_MS` | 選填 | JWT 有效時間，預設 `3600000` ms |

PowerShell 範例：

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your-local-password"
$env:JWT_SECRET = "your-base64-encoded-secret"
```

Repository 內的 JWT default 只供本機開發，不得用於 production。

## 啟動方式

Windows PowerShell／Command Prompt：

```powershell
.\mvnw.cmd spring-boot:run
```

macOS／Linux：

```bash
./mvnw spring-boot:run
```

服務啟動後可先確認：

```http
GET http://localhost:8080/api/health
```

預期 response 為 `OK`。

## manual-test profile 與測試帳號

一般啟動不會自動建立使用者。人工驗證 Login、RBAC 與完整授信流程時，可使用 `manual-test` profile：

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=manual-test"
```

此 profile 會在帳號不存在時建立以下本機測試帳號：

| Username | Password | Role |
| --- | --- | --- |
| `stage5_rm` | `RmPass123!` | RM |
| `stage5_reviewer` | `ReviewerPass123!` | REVIEWER |

這些帳號只供本機人工測試，不應用於 production。完整 current workflow 請參考 `api-test.http`。

## Login 與 JWT 使用方式

先登入：

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "stage5_rm",
  "password": "RmPass123!"
}
```

成功後 response 會包含 `token`。呼叫受保護 API 時加入：

```http
Authorization: Bearer <token>
```

未登入或無效 JWT 回傳 401；已登入但 Role 不符回傳 403。

## API 一覽

| Method | Endpoint | 權限 | 說明 |
| --- | --- | --- | --- |
| `GET` | `/api/health` | Public | Health check |
| `POST` | `/api/auth/login` | Public | 驗證帳密並簽發 JWT |
| `POST` | `/api/companies` | Public（V1 limitation） | 建立 Company |
| `GET` | `/api/companies` | Public（V1 limitation） | 查詢全部 Company |
| `GET` | `/api/companies/{id}` | Public（V1 limitation） | 查詢單一 Company |
| `POST` | `/api/credit-applications` | RM | 建立 DRAFT 申請 |
| `POST` | `/api/credit-applications/{id}/submit` | RM | Submit DRAFT 申請 |
| `GET` | `/api/credit-applications` | Authenticated | Filtering／Pagination／Sorting |
| `POST` | `/api/credit-applications/{id}/approve` | REVIEWER | Approve SUBMITTED 申請 |
| `POST` | `/api/credit-applications/{id}/reject` | REVIEWER | Reject SUBMITTED 申請 |
| `POST` | `/api/credit-limits/{creditLimitId}/drawdowns` | RM | 建立 Drawdown |

## 測試與 Packaging

Windows：

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

macOS／Linux：

```bash
./mvnw test
./mvnw package
```

目前 62 個 automated tests 主要涵蓋 Service Business Rules、State Transition、Audit、Exception contract、Pagination 與 Security RBAC；執行結果為 0 failures／0 errors／0 skipped。`mvnw package` 已成功產生可執行 JAR。GitHub Actions 會在 push／pull request 執行 `test` 與 `package`，Stage 8 remote CI 已完成並通過。

## 目前已知限制

- Company API 目前仍為 public，尚未套用正式 RBAC。
- AuditLog 只有寫入，沒有查詢 API。
- 尚未提供 CreditReview、CreditLimit 或 Drawdown 的 read API。
- 尚未提供修改 DRAFT CreditApplication、User／Role 管理 API。
- CreditReview Entity／Schema 支援 `1:N`，但 V1 service flow 僅允許一次完成審核。
- Automated tests 尚未包含 Testcontainers、真實 PostgreSQL rollback 或 concurrency integration tests。
- Invalid／expired JWT 的自動化整合測試尚未實作；Stage 9 已由人工 REST Client workflow 確認 invalid JWT 回傳 401。
- `credit_applications.created_by` 在目前 migration schema 仍允許 legacy null；這一批 Stage 9 不調整既有 migration。
- Docker、Docker Compose 與 Public Deployment 為 Optional roadmap，尚未實作。

## 專案文件

工程計畫、Domain Model、Business Rules、驗證範圍與後續 Optional roadmap 請參考 [PROJECT_PLAN.md](PROJECT_PLAN.md)。
