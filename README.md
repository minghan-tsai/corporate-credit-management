# Corporate Credit Management System

這是一個以 Java 21、Spring Boot 與 PostgreSQL 建置的企業授信管理後端，模擬公司建檔、授信申請、獨立審核、額度建立與動用流程。

## Project Highlights

- **Maker-Checker**：申請建立者不能審核自己的案件。
- **JWT／RBAC**：依 RM、REVIEWER Role 控制 API 權限。
- **Transaction**：審核、額度、Drawdown 與 AuditLog 在同一交易內處理。
- **`PESSIMISTIC_WRITE`**：鎖定 CreditLimit，避免並行超額動用。
- **AuditLog**：記錄申請建立、Submit、Approve、Reject 與 Drawdown。
- **OpenAPI／Swagger UI**：提供 API 文件與 JWT Authorize 操作介面。
- **62 tests + GitHub Actions CI**：自動測試核心規則、權限與錯誤處理。

## Core Workflow

`Company → CreditApplication → Submit → Approve / Reject → CreditLimit → Drawdown`

合法狀態轉換：

- `DRAFT → SUBMITTED → APPROVED`
- `DRAFT → SUBMITTED → REJECTED`

## Business Rules

- Company 提供建立與查詢；`name` 不可空白，8 碼 `taxId` 必須唯一。
- RM 可以建立與 Submit CreditApplication；新案件為 `DRAFT`，`requestedAmount` 必須大於 0。
- 申請流程只允許 `DRAFT → SUBMITTED`，再由 `SUBMITTED → APPROVED／REJECTED`。
- 審核由 REVIEWER 執行，Maker-Checker 規則禁止經辦人審核自己的案件。
- `approvedAmount` 不得超過 `requestedAmount`，Approve 後建立 CreditLimit，初始可用額度等於核准額度；Reject 必須填寫原因。
- Drawdown 金額必須大於 0，且不得超過 `availableAmount`。
- Approve、Reject 與 Drawdown 都在 Transaction 中執行；Drawdown 另以 `PESSIMISTIC_WRITE` 鎖定額度。

## Tech Stack

- Java 21、Spring Boot 3.5.16、Maven Wrapper
- Spring Web／Validation、Spring Data JPA／Hibernate
- PostgreSQL、Flyway
- Spring Security、BCrypt、JWT（JJWT）
- springdoc-openapi、Swagger UI
- JUnit 5、Mockito、Spring Security Test
- GitHub Actions

## API

OpenAPI／Swagger UI 已完成：

- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

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

## Run Locally

### 環境與資料庫

- JDK 21
- PostgreSQL
- Maven Wrapper 已包含在 Repository 中。

預設連線為 `localhost:5432/corporate_credit_management`，請先建立 database：

```sql
CREATE DATABASE corporate_credit_management;
```

啟動時由 Flyway 套用 migration，Hibernate 使用 `ddl-auto=validate`。

### Environment Variables

| 變數 | 必要性 | 說明 |
| --- | --- | --- |
| `DB_PASSWORD` | 必要 | PostgreSQL password |
| `DB_USERNAME` | 選填 | 預設 `postgres` |
| `SPRING_DATASOURCE_URL` | 選填 | 覆寫預設 PostgreSQL URL |
| `JWT_SECRET` | Production 必要 | Base64 JWT signing key；預設值僅供本機開發 |
| `JWT_EXPIRATION_MS` | 選填 | JWT 有效時間，預設 `3600000` ms |

PowerShell 範例：

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your-local-password"
$env:JWT_SECRET = "your-base64-encoded-secret"
```

### 啟動

```powershell
# Windows
.\mvnw.cmd spring-boot:run

# Windows：啟用本機人工測試帳號
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=manual-test"
```

```bash
# macOS／Linux
./mvnw spring-boot:run
```

啟動後可用 `GET http://localhost:8080/api/health` 檢查服務。

### Demo Credentials 與 Swagger JWT

`manual-test` profile 會在帳號不存在時建立以下本機帳號：

| Username | Password | Role |
| --- | --- | --- |
| `stage5_rm` | `RmPass123!` | RM |
| `stage5_reviewer` | `ReviewerPass123!` | REVIEWER |

測試帳號僅供本機使用。在 Swagger UI 呼叫受保護 API：

1. 呼叫 `POST /api/auth/login` 取得 JWT。
2. 點選 **Authorize**，輸入 response 中的 `token`。
3. 呼叫符合帳號 Role 的受保護 API。

使用 REST Client 時則加入：

```http
Authorization: Bearer <token>
```

完整 request 流程可參考 `api-test.http`。

## Testing

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

- 目前共有 62 個 automated tests，全部通過。
- GitHub Actions 會在 push／pull request 時自動執行 test 與 package。
- 另外用 REST Client 跑過完整授信流程，包含登入、權限、Approve／Reject、Drawdown 與錯誤情境。
- PostgreSQL 端也有確認資料寫入、額度扣減與 AuditLog。

## Known Limitations

- V1 的 Company API 仍是 public，尚未套用 RBAC。
- AuditLog 目前只負責寫入，還沒有查詢 API。
- V1 聚焦核心授信流程，部分查詢與管理 API 尚未補齊；CreditReview service flow 也只允許一次審核。
- 測試目前以 Unit／Security／Manual flow 為主，尚未加入 Testcontainers、真實 PostgreSQL rollback／concurrency，以及 invalid／expired JWT automated integration tests。
- `created_by` 仍保留 legacy null 相容性。
- Docker、Docker Compose 與 Deployment 尚未實作。

## Project Documentation

詳細工程計畫與實作紀錄請參考 [PROJECT_PLAN.md](PROJECT_PLAN.md)。
