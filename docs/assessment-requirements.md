---
title: HealthDekho - Insurance Management System Assessment Specification
status: approved-baseline
audience: Java FDE Capstone Assessment
created: 2026-11-25
updated: 2026-11-25
scope_version: 1.0
---

# HealthDekho - Insurance Management System

## 1. Assessment objective

Develop a greenfield, full-stack insurance management system as a Java FDE Capstone Assessment. The approved implementation supports registration and JWT authentication, customer dashboarding, active product discovery and eligibility, AI-assisted non-claim product recommendations, policy registration, premium payments, and owned-policy search.

**FE-1: Claim Management and AI-Assisted Claim Assessment/Approval is strictly excluded from the current database and API scope.** No claim, claim-status, claim-adjudication, AI claim-assessment, recommendation, approval, rejection, or claim endpoint/table is implemented. Spring AI is used only for product recommendations; it has no claims role.

## 2. Mandatory technology stack

- Java 21
- Spring Boot 3.5.7
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Spring AI
- Bean Validation
- React.js
- Axios

Technology additions used only to support the listed stack are permitted; they must not replace a mandatory technology. Do not introduce a different backend framework, database, frontend framework, or HTTP client.

## 3. Scope

### 3.1 In scope

- FR-1: Customer Registration and Authentication
- FR-2: Insurance Product Management and Discovery
- FR-3: Policy Registration and Lifecycle Management
- FR-4: Premium Payment and Transaction History
- FR-5: Dashboard, Product Recommendation, and Policy Search

### 3.2 Explicitly out of scope

- FE-1: Claim Management and AI-Assisted Claim Assessment and Approval
- Any claim tables, claim DTOs, claim routes, or claim statuses
- Automated claim adjudication, recommendation, approval, or rejection
- Real payment-gateway integration; the assessment records simulated/administrative payment results
- Insurance underwriting beyond age validation and policy issuance rules in this document
- External identity providers, SMS notifications, email delivery, and document-storage services
- Mobile-native clients
- Unspecified insurance products, customer-servicing channels, or analytics dashboards

## 4. Actors

- **Customer:** registers, signs in with JWT, views recommendations/products, registers policies, pays premiums, searches policies, and views an owned dashboard.
- **Administrator:** provisions and manages product configuration.
- **System:** validates requests, enforces customer isolation, generates schedules, calculates payments, records recommendations, and preserves audit/payment history.

## 5. Functional requirements

### FR-1 — Customer Registration and Authentication

1. A customer must register with name, email, phone, date of birth, address, and password.
2. Email must be unique and have a valid email format.
3. Passwords must be stored only as BCrypt hashes.
4. `/api/v1/auth/login` authenticates email/password through Spring Security and returns a JWT for stateless access.
5. Invalid credentials return a non-enumerating failure.
6. Every protected endpoint derives the customer identity from the JWT and strictly rejects access to another customer's resources.
7. The system must let an authenticated customer retrieve and update permitted profile fields. Email uniqueness and password hashing rules continue to apply.

**Acceptance criteria:** successful registration persists a normalized, unique email and BCrypt password hash; duplicate email registration fails; valid login succeeds; invalid credentials fail uniformly; protected endpoints reject anonymous access; a customer cannot access another customer's resources.

### FR-2 — Insurance Product Management and Discovery

1. An administrator can create, view, update, and deactivate insurance products.
2. A product has, at minimum: code, name, description, premium frequency, minimum entry age, maximum entry age, minimum and maximum term months, base premium amount, coverage amount, and active status.
3. Product codes must be unique.
4. Product-level `min_entry_age` and `max_entry_age` govern policy eligibility; global age rules are prohibited.
5. Customers can list active products and inspect a product's public eligibility and premium information.
6. A deactivated product remains readable for existing policy history but cannot be selected for new policies.

**Acceptance criteria:** duplicate product codes fail; inactive products are absent from new-purchase discovery; each product enforces its own age range; product changes do not corrupt historical policy interpretation; required numeric ranges and monetary values pass validation.

### FR-3 — Policy Registration and Lifecycle Management

1. An authenticated customer can purchase an active product by providing `customer_id` implicitly from the authenticated principal, a `product_id`, `start_date`, `term_months`, and payment frequency where applicable.
2. The customer age at `start_date` must be within the selected product's inclusive `min_entry_age` and `max_entry_age`.
3. `term_months` must be within that product's inclusive minimum and maximum term.
4. A customer may have at most one policy for a given product. The system must prevent duplicate `(customer_id, product_id)` at the service and PostgreSQL schema levels.
5. Coverage and premium values used by the policy must be copied from or otherwise reproducibly derived from the product at purchase time so later product edits cannot silently alter an existing policy.
6. Monetary and calculation values must use `BigDecimal`, with scale and rounding explicitly controlled. Floating-point types are forbidden for money.
7. `maturity_date` must equal `start_date + term_months`.
8. A purchased policy receives a stable policy number, an issue date, and a lifecycle status.
9. Customers can list and view their own policies; administrators can inspect policy records.
10. A policy cannot be purchased for an inactive product or with an invalid start date/term.

**Acceptance criteria:** an eligible purchase creates one policy; re-purchase of the same customer/product pair fails; all calculations are deterministic with `BigDecimal`; maturity date is exact; age and term limits are product-specific; later product edits do not mutate stored policy financial values.

### FR-4 — Premium Payment and Transaction History

1. The system generates installments from `start_date` at the product frequency: `MONTHLY` +1 month, `QUARTERLY` +3 months, and `YEARLY` +12 months, up to `term_months`.
2. Each installment has `grace_period_end_date = due_date.plusDays(15)`.
3. An authenticated customer may pay an owned installment. Payment is immediately simulated as confirmed and appends an immutable `premium_payment` row with a unique `TXN-HD-XXXXXX` reference.
4. Payment after `grace_period_end_date` incurs a mandatory penalty of `scheduled_amount * 0.10`, rounded to two decimals and recorded in `premium_payment.penalty_amount`.
5. Partial payments are allowed and set the installment to `PARTIALLY_PAID`; the remaining balance remains payable.
6. Amount paid above remaining balance plus any applicable penalty is rejected.
7. `customer_policy.status` becomes `PAID` only when all installments are `PAID`.
8. Premium payment ledger history is immutable; corrections append a new ledger/audit record.
9. Customers can view only their own payment history; administrators may inspect history for assessment/operations support.

**Acceptance criteria:** due-date and grace-period payments have no penalty; the first day after grace carries exactly 10% of scheduled amount; partial payments preserve exact balances; overpayments fail transactionally; immediate confirmation creates one immutable ledger row; a fully paid schedule can transition its policy to `PAID` only after every installment is `PAID`.

### FR-5 — Dashboard, Recommendation, and Policy Search

1. The logged-in customer dashboard summarizes owned active policies, amount due, next due date, paid/partially paid installments, and policy statuses.
2. Product recommendations use Spring AI only to explain/rank eligible insurance products; they may not bypass deterministic eligibility.
3. Recommendation candidates must be `active = true`, not already registered by the customer, and satisfy product-specific age eligibility.
4. The system must persist recommendation provenance in `ai_recommendation` without introducing claim behavior.
5. Customers may search only policies owned by the authenticated principal using supported policy/product/status/date filters.
6. FE-1 Claim Management is excluded from the database and API.

**Acceptance criteria:** dashboard and searches contain no other customer's records; only deterministic eligible products can be recommended; recommendation output identifies that it is non-binding product guidance; no claim route, entity, table, or screen exists.

## 6. Business rules

| ID | Rule |
|---|---|
| BR-1 | A customer email is unique. |
| BR-2 | Passwords are persisted as BCrypt hashes only. |
| BR-3 | A customer can have at most one policy per product. PostgreSQL must enforce `UNIQUE (customer_id, product_id)` and the service layer must detect the conflict. |
| BR-4 | Eligibility uses the selected product's inclusive `min_entry_age` and `max_entry_age`. |
| BR-5 | `term_months` must be within the selected product's inclusive minimum and maximum. |
| BR-6 | All monetary values, rates, premiums, fees, payments, balances, and recommended premium values use `BigDecimal`. |
| BR-7 | Monetary persistence and calculations use a consistent scale and explicit rounding; the initial required rule is two decimal places with `HALF_UP` unless PostgreSQL/contract tests establish a stricter policy. |
| BR-8 | `maturity_date = start_date.plusMonths(term_months)`. |
| BR-9 | Premium grace period ends at `grace_period_end_date = due_date.plusDays(15)`. |
| BR-10 | A payment after the grace-period end date carries a mandatory 10% penalty calculated as `scheduled_amount * 0.10` and persisted in `premium_payment.penalty_amount`. |
| BR-11 | Partial payments are allowed and represented by `PARTIALLY_PAID`; the remaining balance stays payable. |
| BR-12 | Overpayments beyond `remaining balance + applicable penalty` are rejected. |
| BR-13 | Premium payment ledger history is immutable; corrections are additive. |
| BR-14 | A Policy becomes `PAID` only when every installment is `PAID`. |
| BR-15 | FE-1 is excluded: no current Claim database table, API, DTO, screen, or story. |
| BR-16 | Recommendation candidates must be active, not already held by the customer, and age-eligible. Deterministic eligibility cannot be overridden by AI. |

## 7. Database requirements

PostgreSQL is the system of record. Exactly seven domain tables are required for the current assessment baseline:

1. `customer`
2. `insurance_product`
3. `customer_policy`
4. `premium_schedule`
5. `premium_payment`
6. `ai_recommendation`
7. `audit_event`

### 7.1 Table contracts

#### `customer`
- Identity/profile, unique normalized email, BCrypt password hash, role, timestamps.

#### `insurance_product`
- Unique code; premium frequency (`MONTHLY`, `QUARTERLY`, `YEARLY`); product-specific age and term limits; premium/coverage; active flag.

#### `customer_policy`
- Unique policy number; customer/product foreign keys; `UNIQUE(customer_id, product_id)`; start/term/maturity dates; financial snapshots; statuses `ACTIVE`, `PAID`, `MATURED`, `CANCELLED`.

#### `premium_schedule`
- Policy foreign key; installment sequence; due date; `grace_period_end_date`; scheduled amount; paid, penalty, and remaining amounts; statuses `DUE`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED`.

#### `premium_payment`
- Immutable payment ledger; unique `TXN-HD-XXXXXX` reference; schedule/policy/customer references; amount, `penalty_amount`, balance, immediate `CONFIRMED` result, timestamp. No UPDATE/DELETE application path.

#### `ai_recommendation`
- Customer, product, request model/version, non-binding explanation, eligibility evidence, ranking, created timestamp; no claim data or assessment fields.

#### `audit_event`
- Append-only security/domain events, actor, resource, action, safe JSON metadata, correlation ID, timestamp; no passwords/tokens/raw sensitive payloads.

### 7.2 Calculation and schedule rules

- Schedule generation starts from policy `start_date` and continues at the product frequency until `term_months`: `MONTHLY` `+1 month`, `QUARTERLY` `+3 months`, `YEARLY` `+12 months`.
- `grace_period_end_date = due_date.plusDays(15)`.
- `maturity_date = start_date.plusMonths(term_months)`.
- A post-grace payment records exactly `scheduled_amount * 0.10` in `premium_payment.penalty_amount`.
- The policy may transition to `PAID` only when all schedules are `PAID`.

## 8. Sample seed data

Seed data is for development/demo and must not contain real personal data. Passwords shown below are input values for provisioning only; the database receives BCrypt hashes.

### Customers

| email | name | role | date_of_birth | address | seed password input |
|---|---|---|---|---|---|
| admin@healthdekho.test | HealthDekho Administrator | ADMIN | 1985-01-15 | Demo administrative address | `ChangeMe_Admin_2026!` |
| aisha@example.test | Aisha Rao | CUSTOMER | 1996-06-20 | Demo address, Pune | `ChangeMe_Aisha_2026!` |
| rohan@example.test | Rohan Shah | CUSTOMER | 1988-11-05 | Demo address, Mumbai | `ChangeMe_Rohan_2026!` |

All seed password inputs must be replaced before any non-demo deployment and must be encoded with BCrypt at provisioning time.

### Insurance products

| code | name | frequency | min age | max age | min term | max term | base premium | coverage | active |
|---|---|---:|---:|---:|---:|---:|---:|---:|---|
| HD-HEALTH-ESSENTIAL | Health Essential | MONTHLY | 18 | 60 | 12 | 60 | 1500.00 | 500000.00 | true |
| HD-HEALTH-FAMILY | Health Family Plus | MONTHLY | 21 | 55 | 12 | 120 | 3200.00 | 1500000.00 | true |
| HD-TERM-20 | Term Secure 20 | YEARLY | 18 | 50 | 120 | 240 | 12000.00 | 3000000.00 | true |

### Example policy

- Customer: `aisha@example.test`
- Product: `HD-HEALTH-ESSENTIAL`
- Start: `2026-11-01`
- Term: `12` months
- Maturity: `2027-11-01`
- Premium: `1500.00`
- Coverage: `500000.00`
- Status: `ACTIVE`

### Example payment cases

| due date | grace end | payment date | scheduled | paid | penalty | resulting installment balance |
|---|---|---|---:|---:|---:|---:|
| 2026-12-01 | 2026-12-16 | 2026-12-15 | 1500.00 | 1500.00 | 0.00 | 0.00 |
| 2027-01-01 | 2027-01-16 | 2027-01-17 | 1500.00 | 1500.00 | 150.00 | 150.00 |
| 2027-02-01 | 2027-02-16 | 2027-02-10 | 1500.00 | 500.00 | 0.00 | 1000.00 |

The second row is settled only when the remaining 150.00 penalty is paid; the ledger preserves both transactions.

### Example product recommendation

- Customer: Aisha Rao, age-eligible for `HD-HEALTH-ESSENTIAL`
- Candidate must be active and not already registered by Aisha.
- Spring AI may rank and explain the deterministic eligible candidate; it cannot recommend an ineligible or duplicate product.
- Recommendation records only non-binding product guidance and have no claim fields.

## 9. Non-functional requirements

- **Security:** Spring Security authentication/authorization, BCrypt password hashing, input validation, least-privilege resource access, and secure handling of secrets.
- **Validation:** Jakarta Bean Validation at API/domain boundaries plus database constraints for critical invariants.
- **Integrity:** transactional consistency for policy purchase and payment balance updates; concurrency-safe duplicate and balance handling.
- **Data accuracy:** `BigDecimal` for financial values, explicit scale/rounding, database NUMERIC columns, and deterministic date calculations.
- **Auditability:** immutable premium payment and audit-event histories; no destructive normal application path.
- **Maintainability:** layered Spring architecture and a React UI using Axios against the Spring Web API.
- **Reliability:** expected validation/conflict errors return deterministic, non-sensitive HTTP responses.
- **Testability:** automated tests must cover FR-1 through FR-5, critical business-rule boundaries, and ownership/authorization failures.
- **Usability/accessibility:** clear validation messages and keyboard-accessible React forms; exact visual compliance is not specified in the assessment baseline.
- **Observability:** log application events and errors without passwords, tokens, or sensitive customer values.

Performance, availability, recovery objectives, accessibility conformance level, retention policy, and supported browsers are not specified and remain open assessment clarifications.

## 10. API expectation

The architecture phase must define API endpoints and DTOs. The baseline expectation is a versioned REST API exposed through Spring Web, with request Bean Validation and no direct entity binding from untrusted requests. The React application consumes it through Axios.

## 11. Acceptance scope and demonstration

A successful assessment demonstration must prove:

1. FR-1 through FR-5 work end to end.
2. Duplicate customer email, duplicate product code, and duplicate `(customer_id, product_id)` are rejected.
3. Product-specific age rules are enforced.
4. Financial calculations use `BigDecimal` and correct scale/rounding.
5. Policy maturity date is calculated from `start_date + term_months`.
6. Grace and 10% late-fee rules work at boundaries.
7. Partial payments are supported and balances remain accurate.
8. Passwords are BCrypt hashes, never plaintext.
9. Transaction and audit-event histories cannot be normally edited or deleted.
10. FE-1 is absent from the database, API, UI, and stories.

## 12. Ambiguities and required clarifications

The following details were not provided in the user-supplied scope and must be confirmed before architecture or implementation is finalized:

- A-1: Exact endpoint names/payloads and API authentication mechanism.
- A-2: Defined status values and permitted transitions for policies; claim states are out of scope.
- A-3: Premium-frequency behavior and installment schedule generation rules, including first due date.
- A-4: Whether a 10% late fee is capitalized into the schedule balance and whether later missed payments apply 10% once or repeatedly.
- A-5: Payment simulation behavior: immediate success, operator-entered status, or another assessment convention.
- A-6: Claim coverage is irrelevant because FE-1 and all Claim Management are excluded.
- A-7: Data retention, audit retention duration, and whether hard deletion is prohibited for all entities.
- A-8: Required test coverage threshold, performance SLA, browser support, and deployment target.
- A-9: Currency/locale (seed data assumes no currency conversion and uses generic two-decimal monetary units).
- A-10: Whether Spring AI must be actively integrated beyond dependency/configuration despite FE-1 being excluded.

These ambiguities are now resolved for current scope as follows:

- JWT stateless Spring Security login at `/api/v1/auth/login` and strict JWT-derived customer isolation.
- Policy states are `ACTIVE`, `PAID`, `MATURED`, `CANCELLED`; FE-1 is excluded entirely.
- Frequency schedules, 15-day grace, and maturity calculation are fixed as described in §7.2.
- Post-grace penalty is 10% of scheduled amount and applies to the payable total; partial payments and overpayment rejection are fixed.
- Payments are immediately confirmed into an immutable `premium_payment` ledger with unique `TXN-HD-XXXXXX` references.
- Product discovery/recommendation eligibility is active, not-yet-registered, and age-matched.

Remaining non-blocking clarifications: currency/locale, deployment environment configuration, exact test-coverage threshold, and external AI model/provider configuration. No claim-related ambiguity remains because claims are outside current scope.

## 13. Approval gates

- **Planning gate:** approve the assessment baseline, product brief, and PRD.
- **Architecture gate:** approve technical decisions after planning approval.
- **Implementation gate:** generate no application code before the architecture phase is complete and approved.
- **Scope gate:** reject changes that add FE-1 or replace a mandatory stack item unless the assessment owner formally changes the specification.
