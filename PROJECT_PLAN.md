# Corporate Credit Management System — Project Plan

## Project Overview

Corporate Credit Management System is a portfolio-oriented Java/Spring Boot backend that simulates a deliberately small but deep subset of a bank-internal corporate lending system. It focuses on the lifecycle from corporate-customer registration through credit application, independent review, approved limit creation, and drawdown. It is not intended to be a complete core banking platform.

- Artifact/folder: `corporate-credit-management`
- Base package: `com.minghan.credit`
- Packaging: executable JAR
- Target completion date: 2026-09-10
- Delivery principle: **small and deep**

## Goals

- Demonstrate practical Java 21 and Spring Boot backend engineering.
- Model meaningful corporate-credit business rules instead of CRUD alone.
- Apply REST API design and a clear layered architecture.
- Use PostgreSQL with explicit, versioned schema migration.
- Demonstrate transaction boundaries and rollback guarantees.
- Implement authentication, authorization, RBAC, and Maker-Checker controls in their planned stages.
- Maintain a distinct audit trail for business operations.
- Verify core rules through proportionate automated and manual testing.
- Add reproducible packaging and CI only after the core system is sound.

## Target Job / Technical Positioning

The project targets financial-IT and bank corporate-banking/credit backend roles. It is designed to evidence Java, Spring Boot, REST, SQL/PostgreSQL, JPA/Hibernate, transactions, authentication/authorization, RBAC, Maker-Checker/segregation of duties, validation, exception handling, audit trails, automated testing, Docker, and CI/CD.

Priority is given to explainable business logic, data consistency, security boundaries, and tests. Public deployment is optional and must not displace those priorities.

## Business Domain

### Core business domains

1. **Company** — corporate customer.
2. **CreditApplication** — request for corporate credit.
3. **CreditReview** — approval or rejection review record.
4. **CreditLimit** — approved credit facility/limit.
5. **Drawdown** — utilization of an approved limit.

### System-control domains

6. **User** — internal bank system user.
7. **Role** — one of RM, REVIEWER, or ADMIN.

### Cross-cutting domain

8. **AuditLog** — business-operation audit record.

These domains are plans only during Stage 0; no domain implementation is included in this stage.

## Roles

### RM

Planned permissions:

- Create a corporate customer.
- Create a credit application.
- Modify an application that the RM owns while it is in DRAFT.
- Submit an application.
- Query credit cases.
- Create a drawdown.

RM must not approve or reject applications.

### REVIEWER

Planned permissions:

- Query credit applications.
- Approve or reject a submitted application.
- View reviews and approved credit limits.

Maker-Checker is an independent business rule, not merely a side effect of assigning different endpoint permissions to RM and REVIEWER. Even when a user's role authorizes review, the Service layer must verify that the reviewer and `CreditApplication.createdBy` are not the same User before allowing Approve or Reject. Endpoint authorization alone is insufficient to enforce segregation of duties.

Whether User and Role will use a single-role or multi-role relationship remains an explicit design decision for the Security/RBAC stage. Stage 0 does not decide or implement that relationship, and the Maker-Checker rule must remain valid under either model.

### ADMIN

Planned responsibilities:

- Manage users and roles.
- Query audit logs.

ADMIN is not a financial-business super user and must not automatically gain unrestricted application, review, limit, or drawdown powers.

## Core Workflow

`Company → CreditApplication → Submit → Review → Approve / Reject → CreditLimit → Drawdown`

V1 application states:

- `DRAFT`
- `SUBMITTED`
- `APPROVED`
- `REJECTED`

Legal V1 transitions:

- `DRAFT → SUBMITTED → APPROVED`
- `DRAFT → SUBMITTED → REJECTED`

V1 intentionally excludes complex multi-stage approval.

## Business Rules

### CreditApplication

- `requestedAmount` must be greater than zero.
- Only a DRAFT application can be modified.
- Only a DRAFT application can be submitted.
- Only a SUBMITTED application can be approved or rejected.

### CreditReview

- Only REVIEWER may review an application.
- Maker-Checker/segregation of duties is a Service-layer business rule: even when role authorization permits review, the reviewer must not be the same User as `CreditApplication.createdBy`.
- Endpoint authorization and RBAC checks do not replace the reviewer-versus-creator validation.
- `approvedAmount` must be greater than zero for approval.
- `approvedAmount` must not exceed `requestedAmount`.
- Rejection requires a reason.

### CreditLimit

- A CreditLimit may be created only for an APPROVED CreditApplication.
- One CreditApplication may have at most one CreditLimit.
- `usedAmount` must not exceed `approvedAmount`.

### Approval and rejection transaction boundaries

A successful Approve operation must perform all of the following within one transaction:

1. Transition CreditApplication from `SUBMITTED` to `APPROVED`.
2. Create CreditReview.
3. Create the application's unique CreditLimit.
4. Create AuditLog.

If any step fails, the entire Approve transaction must roll back. It must not leave a partially updated application, review, limit, or audit record.

A Reject operation must perform all of the following within one consistent transaction boundary:

1. Transition CreditApplication from `SUBMITTED` to `REJECTED`.
2. Create CreditReview.
3. Create AuditLog.

If any Reject step fails, the transaction must roll back and must not leave partially successful data.

### Drawdown

- `amount` must be greater than zero.
- The CreditLimit must be effective and not expired.
- Drawdown `amount` must not exceed `availableAmount`.
- A successful drawdown must atomically update CreditLimit, create Drawdown, and create AuditLog.
- If any step fails, the entire operation must roll back.

## Audit Planning

V1 planned audit actions:

- `CREATE_COMPANY`
- `CREATE_APPLICATION`
- `SUBMIT_APPLICATION`
- `APPROVE_APPLICATION`
- `REJECT_APPLICATION`
- `CREATE_DRAWDOWN`

AuditLog will contain at least:

- `userId`
- `action`
- `targetType`
- `targetId`
- `timestamp`

Application logs are operational/diagnostic logs. Audit logs are durable business records of who performed a relevant action against which target and when. They are separate concepts and must not be treated as interchangeable.

## Data Model

Planned relationships:

- Company `1:N` CreditApplication
- CreditApplication `1:N` CreditReview
- CreditApplication `1:0..1` CreditLimit
- CreditLimit `1:N` Drawdown

Planned User references:

- `CreditApplication.createdBy`
- `CreditReview.reviewedBy`
- `Drawdown.createdBy`
- `AuditLog.user`

Precise ownership, fetch, cascade, identifier, indexing, locking, and monetary-column decisions will be made and reviewed in their scheduled stages rather than assumed during Stage 0.

## Planned APIs

The following is a planning-level API map, not a Stage 0 implementation or final contract:

| Capability | Planned method/path | Primary role |
| --- | --- | --- |
| Create company | `POST /api/companies` | RM |
| Query companies | `GET /api/companies` | Authorized internal user |
| Create credit application | `POST /api/credit-applications` | RM |
| Update owned draft | `PUT /api/credit-applications/{id}` | RM |
| Submit application | `POST /api/credit-applications/{id}/submit` | RM |
| Query applications | `GET /api/credit-applications` | RM, REVIEWER |
| Approve application | `POST /api/credit-applications/{id}/approve` | REVIEWER |
| Reject application | `POST /api/credit-applications/{id}/reject` | REVIEWER |
| View reviews | `GET /api/credit-applications/{id}/reviews` | REVIEWER |
| View credit limit | `GET /api/credit-applications/{id}/credit-limit` | REVIEWER and authorized business users |
| Create drawdown | `POST /api/credit-limits/{id}/drawdowns` | RM |
| Manage users/roles | `/api/admin/users`, `/api/admin/roles` | ADMIN |
| Query audit logs | `GET /api/admin/audit-logs` | ADMIN |

Request/response DTOs, response codes, filtering, pagination, idempotency, concurrency control, and error contracts will be specified when each feature is designed. No controller exists in Stage 0.

## Architecture

The planned structure is layered:

`Controller → Service → Repository → PostgreSQL`

It is supported by DTO, Entity, Validation, Exception, and Security concerns.

Coding rules:

1. Controller must not contain core business logic.
2. Controller must not call Repository directly.
3. API requests and responses must not expose Entity directly.
4. Transaction boundaries belong primarily in Service.
5. Core business rules require tests.
6. Passwords, JWTs, and secrets must never be logged.
7. Avoid broad or meaningless try/catch blocks.
8. Do not add a framework that cannot be clearly justified.

The application remains a modular monolith for V1; microservices are out of scope.

## Tech Stack

### Initialized in Stage 0

- Java 21
- Maven
- Spring Boot 3.5.16, executable JAR
- Spring Web
- Spring Data JPA
- Spring Security dependency only; no security implementation
- Spring Validation
- PostgreSQL JDBC Driver
- Flyway Core and Flyway PostgreSQL support
- Spring Boot Test dependency only; no formal test cases

### Planned for later stages

- JWT authentication
- OpenAPI/Swagger
- JUnit 5 and Mockito
- Testcontainers as a high-priority optional enhancement
- SLF4J and Logback through Spring Boot defaults
- Docker Compose
- GitHub Actions
- Spring Batch or scheduling only if later justified and time permits

Lombok is not a Stage 0 dependency and should not be relied on heavily. Redis, Kafka, Kubernetes, Elasticsearch, and microservices are excluded from V1.

## Database Strategy

- Database: PostgreSQL
- ORM: Spring Data JPA, Jakarta Persistence/JPA, and Hibernate
- Schema migration: Flyway

Principles:

- Flyway owns creation and modification of database schema.
- Hibernate owns object-relational mapping and validates the schema.
- The intended formal setting is `spring.jpa.hibernate.ddl-auto=validate` after schema and environment configuration exist.
- `ddl-auto=update` must not be used as the formal schema-management strategy.
- No business schema or formal migration is created during Stage 0.
- Database connection details must be externalized and secrets must not be committed.

Because the Stage 0 skeleton intentionally has no PostgreSQL connection configuration or migration, normal application startup with JPA/Flyway auto-configuration will require later environment setup. Build and compilation must remain independently verifiable.

## Testing Strategy

No formal test cases are created in Stage 0.

### Unit tests

- JUnit 5 and Mockito.
- Focus primarily on Service business rules and state transitions.

### Integration tests

- Spring Boot Test.
- Cover core API, Repository, and database flows.

### Security tests

At minimum verify:

- Unauthenticated request returns 401.
- RM calling Approve returns 403.
- REVIEWER performing a legal Approve succeeds.

### Transaction tests

When approval fails at any step, verify all of the following roll back together:

- CreditApplication does not remain incorrectly APPROVED.
- CreditReview is not partially inserted.
- CreditLimit is not partially inserted.
- AuditLog is not partially inserted.

When rejection fails at any step, verify CreditApplication, CreditReview, and AuditLog remain consistent and no partial result is committed.

When drawdown fails, verify all of the following:

- CreditLimit is not incorrectly updated.
- Drawdown is not inserted.
- AuditLog is not inserted.

Testcontainers is a high-priority enhancement but may be omitted if schedule pressure requires it. The project does not target 100% coverage; it targets strong coverage of risk-bearing rules and boundaries.

## Docker / CI Strategy

Stage 0 does not implement Docker or CI.

- Early development: local Java plus local PostgreSQL.
- Later packaging: Docker Compose with Spring Boot and PostgreSQL.
- CI platform: GitHub Actions.
- Minimum CI commands: `mvn test` and `mvn package`.
- Public deployment is optional.

Deployment work must not displace business logic, transaction correctness, security, or testing.

## AI / Codex Collaboration Rules

Codex must not write the entire core implementation on the user's behalf.

- The user owns understanding and implementation of core business logic.
- For a core technology being encountered for the first time, the user learns it and writes the first version.
- Codex primarily assists with review, boilerplate, repetitive work, automation, documentation, and Git operations.
- Codex must not introduce a major framework or change the architecture without explicit agreement.
- Before modifying core code, Codex explains the purpose of the change.
- After modifications, Codex lists changed files and the reason for each.
- The user reviews all core diffs.

On their first implementation, Codex must not build these complete features from scratch:

- JPA relationships
- Transactions
- Spring Security
- Business rules
- Exception handling
- Integration tests

These rules require the assistant to pause at the appropriate learning/review boundary even when code generation would be faster.

## Stage Plan

Assumption: dates refer to 2026 and all times are Asia/Taipei. Expected effort is approximately 5–6 hours per development day. Final deadline is 2026-09-10.

| Stage | Dates | Planned outcome |
| --- | --- | --- |
| Stage 0 | 8/21–8/22 | Planning, environment, documentation, minimal project initialization |
| Stage 1 | 8/23 | Spring Boot base skeleton |
| Stage 2 | 8/24–8/25 | PostgreSQL, JPA, Flyway, Company |
| Stage 3 | 8/26–8/27 | CreditApplication and Submit |
| Stage 4 | 8/28–8/29 | CreditReview, Approve/Reject, CreditLimit |
| Stage 5 | 8/30–8/31 | Spring Security, JWT, RBAC, Maker-Checker |
| No computer development | 9/1–9/2 | No scheduled implementation work |
| Stage 6 | 9/3–9/4 | Drawdown and transaction behavior |
| Stage 7 | 9/5 | Audit, exception handling, filtering, pagination |
| Stage 8 | 9/6–9/7 | Unit, integration, security tests, and CI |
| Stage 9 | 9/8–9/10 | Docker, documentation, final verification, release |

No large feature may be added after 2026-09-08.

## Definition of Done

Each stage must confirm at least:

1. Code is complete for the agreed stage scope.
2. Business rules are verified.
3. Core tests pass.
4. Manual verification passes.
5. `PROJECT_PLAN.md` is updated.
6. Build passes.
7. Git status is reviewed.

Planned stage closeout:

1. `git diff`
2. `git diff --check`
3. `mvn test`
4. `mvn package`
5. `git status`
6. Commit
7. Push
8. Verify CI
9. Tag when appropriate

Stage tags are `v1-stage-0` through `v1-stage-9`. The final release tag is `v1.0.0`.

## Scope Exclusions

V1 does not include:

- Real credit scoring
- Financial-statement analysis
- Collateral valuation
- Credit-bureau integration
- Basel calculations
- IFRS 9
- Complete repayment or interest model
- Delinquency management
- Real movement of funds
- Complex multi-stage approval
- Redis
- Kafka
- Kubernetes
- Elasticsearch
- Microservices

Also excluded from Stage 0 are all formal business entities, controllers, services, repositories, queries, business logic, security/JWT/RBAC implementation, transactions, formal tests, migrations, Docker, CI, batch processing, and messaging.

## Current Status

**Stage 0 — Project Planning and Initialization**

Stage 0 contains only:

- A minimal Java 21 / Maven / Spring Boot 3.x application entry point.
- Build dependencies required by the approved future stack.
- Minimal application naming configuration.
- Project planning and introductory documentation.
- Repository ignore rules.

Stage 0 scope has passed manual review and is approved for Git/GitHub closeout. No Stage 1 business or technical feature implementation has begun; Stage 1 must start only as a separate, explicitly reviewed step after this closeout.
