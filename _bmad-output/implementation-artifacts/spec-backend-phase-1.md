---
title: HealthDekho Backend Phase 1
status: in-progress
route: dispatch
created: 2026-11-25
updated: 2026-11-25
baseline_commit: NO_VCS
---

# Spec: HealthDekho Backend Phase 1

## Intent

<frozen-after-approval>
Build the complete PostgreSQL database layer and Java 21 Spring Boot 3.5.7 backend under `backend/` for FR-1 through FR-5 and US-01 through US-07. Implement Maven, Flyway schema and seed data, stateless JWT security, product discovery, bounded Spring AI recommendations with deterministic fallback, policy registration and installment generation, dashboard/search, atomic partial/full premium payments, and global structured error handling. Use exactly the seven tables named by the current implementation instruction, replace any conflicting prior table naming with those names, preserve strict customer isolation, use records for all DTOs, BigDecimal for all money, and keep FE-1/claim management absent. Finish by compiling tests with `mvn test-compile`.
</frozen-after-approval>

## Code Map

- No application source exists; `backend/` will be greenfield.
- `docs/architecture.md` points to `_bmad-output/architecture.md` for C4, security, REST, transaction, and AWS invariants.
- `_bmad-output/epics-and-stories.md` defines US-01 through US-07 and AUTH/DASH/DISC/AI/REG/PAY/SEA acceptance prefixes.
- `_bmad-output/sprint-status.yaml` currently blocks code only pending architecture approval; the user's current approval satisfies this implementation gate.
- `docs/assessment-requirements.md` is the domain baseline, but the current user instruction explicitly revises the physical seven-table names to `customer`, `insurance_product`, `customer_policy`, `premium_installment`, `premium_payment`, `ai_recommendation_request`, and `ai_recommendation_item`.
- Do not modify `_bmad/`, `.agents/`, planning artifacts, or generated source under `docs/` during implementation except if a verified implementation fact requires documentation.

## Plan

### 1. Maven and runtime scaffold

Create `backend/pom.xml`, `HealthDekhoApplication.java`, Maven wrapper-compatible standard structure, `application.yml`, H2 test configuration, and package root `com.healthdekho.ims`.

- Java 21; Spring Boot 3.5.7.
- Dependencies: Web, Security, Data JPA, Validation, Actuator, Flyway PostgreSQL, PostgreSQL, JJWT 0.12.6, Spring AI OpenAI starter, Lombok, test, security-test, H2.
- Jacoco enforces at least 80% service line coverage and 75% critical service branch coverage. Configure the rule around `com.healthdekho.ims.service.*` rather than blocking infrastructure/DTO classes.

### 2. PostgreSQL schema and seed

Create `V1__init_healthdekho_schema.sql` with exactly seven domain tables:

1. `customer`
2. `insurance_product`
3. `customer_policy`
4. `premium_installment`
5. `premium_payment`
6. `ai_recommendation_request`
7. `ai_recommendation_item`

Add PKs, FKs, requested unique constraints, dates, NUMERIC(19,2), checks, indexes, and immutable-payment protection suitable for PostgreSQL. Do not create claim/FE-1 tables.

Create `V2__seed_healthdekho_data.sql` with BCrypt-hashed users, active/inactive products across HEALTH, TERM_LIFE, CRITICAL_ILLNESS, FAMILY_FLOATER, owned policy samples, all requested installment states, and payment history. H2 verification must either run PostgreSQL-compatible SQL or disable Flyway where profile-specific schema setup is used.

### 3. Domain and persistence

Create JPA entities, enums, repositories, mappers, and all request/response DTO records. Entity mutability may exist for managed persistence, but every API DTO must be a Java `record`.

Policy state: `ACTIVE`, `PAID`, `MATURED`, `CANCELLED`. Installment state: `PENDING`, `PAID`, `PARTIALLY_PAID`, `OVERDUE`. Payment state: `CONFIRMED`.

### 4. Security and errors

Implement JWT service, login/authentication flow, bearer filter, Spring Security stateless chain, BCrypt password encoding, principal-derived ownership, and `/api/v1/auth/login`, `/api/v1/auth/me`.

Implement `@RestControllerAdvice` with structured safe errors for 400, 401, 403, 404, 409, 422, and 500, including correlation ID/path/field errors without stack traces or secrets.

### 5. Functional APIs

- `GET /api/v1/policies/my`: owned dashboard/policy summaries.
- `GET /api/v1/policies/search`: owned pagination/filter search.
- `GET /api/v1/products/available`: active, age-eligible, not-yet-registered products only.
- `POST /api/v1/policies/register`: deterministic `POL-HD-YYYY-XXXX`, age/term/duplicate checks, snapshot values, maturity calculation, total premium, and installment schedule with 15-day grace.
- `GET /api/v1/payments/policies/{policyNumber}`: owned policy installment/payment view.
- `POST /api/v1/payments/process`: atomic locked balance update, mandatory 10% post-grace fee, partial/full behavior, overpayment rejection, duplicate prevention, transaction reference, immutable ledger, and policy `PAID` only when all installments are paid.

### 6. Recommendation service

`POST /api/v1/recommendations` filters products deterministically by active status, age, and existing policy ownership, then attempts Spring AI `ChatClient`. Validate every AI `product_code` against the bounded database candidates; discard invalid codes. Persist request/outcome in `ai_recommendation_request` and accepted items in `ai_recommendation_item`. If no OpenAI key, timeout/error, malformed output, or no valid codes, return a deterministic ranking safely without claims behavior.

### 7. Verification

Add focused unit/service tests for authorization-independent business services, schedule generation, age discovery, AI deterministic fallback, recommendation validation, and payment penalties/partial/overpayment/all-paid transitions. Run `mvn test-compile`; if dependencies/tooling permit, run tests and report gaps.

## Design Notes

- Physical schema follows the explicit current user instruction, which supersedes the earlier architecture's `premium_schedule`/`audit_event` names while retaining the architecture's seven-table and no-claims principles.
- The AI provider must be optional at startup. Auto-configuration that requires a key must not make the backend unusable; deterministic mode must remain testable.
- Money is never `double` or `float`. Payment eligibility is authoritative on the server.
- JWT customer subject/email is resolved to `customer`; arbitrary customer IDs are not trusted.
- Flyway migrations are forward-only; development seed changes belong in seed migration revisions, not edits after an actual release.

## Test Plan

- Compile all production and test Java 21 sources.
- Verify auth password/JWT and role/ownership-negative behavior.
- Verify available products exclude inactive, age-ineligible, and already-held products.
- Verify frequency schedule count/date/maturity and 15-day grace boundaries.
- Verify day-of-grace no penalty, next-day 10% scheduled-amount penalty, partial balance, overpayment failure, immutable payment reference, duplicate prevention, and all-schedules-paid policy transition.
- Verify recommendation invalid product codes cannot escape deterministic candidates and provider absence/error falls back.
- Verify no claim classes, endpoints, migrations, or FE-1 behavior.

## Irreversibles

- Flyway migration `V1` becomes a schema contract once applied.
- Seed passwords and IDs are irreversible development-data assumptions if consumers start relying on them.
- Production JWT/AI secrets and Flyway database migration execution must be environment-controlled.

## Footprint

New `backend/` Maven project containing approximately 45-65 Java/config/resource files, two Flyway migrations, and focused service/security/API tests. No existing application code is replaced.

## Open Questions

None. The user's latest instruction resolves the implementation details and the planning artifacts are approved.

## Task List

- [ ] Maven scaffold and externalized configuration
- [ ] Seven-table Flyway schema and seed
- [ ] Domain entities, enums, repositories, record DTOs
- [ ] JWT Spring Security and global errors
- [ ] Auth, dashboard, discovery, recommendation, registration, payment APIs
- [ ] Service tests and Jacoco rules
- [ ] `mvn test-compile` verification

## Review Checklist

- [ ] No claim/FE-1 artifact exists
- [ ] Exactly seven migration tables exist
- [ ] Every requested endpoint exists
- [ ] DTOs are records and money is BigDecimal
- [ ] Ownership and role checks cover each endpoint
- [ ] AI provider absence/error is safe and deterministic
- [ ] Payment mutation is atomic and immutable ledger preserved
- [ ] `mvn test-compile` passes
