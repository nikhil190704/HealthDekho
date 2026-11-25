---
title: HealthDekho Epics and User Stories
status: final-for-implementation-planning
created: 2026-11-25
updated: 2026-11-25
stepsCompleted: [validate-prerequisites, design-epics, create-stories]
inputDocuments:
  - docs/assessment-requirements.md
  - _bmad-output/prd.md
  - _bmad-output/ux-design-specification.md
  - _bmad-output/architecture.md
---

# HealthDekho - Epics and User Stories

## 1. Requirements inventory

### Functional requirements

- **FR-1:** Customer registration, BCrypt passwords, JWT login at `/api/v1/auth/login`, Spring Security, and strict ownership.
- **FR-2:** Admin product maintenance and customer discovery of active, age-eligible, not-held products.
- **FR-3:** One policy per customer/product, snapshot values, `BigDecimal`, frequency-based schedules, and calendar maturity.
- **FR-4:** 15-day grace, 10% post-grace penalty, partial payments, no overpayment, immediate immutable ledger, policy `PAID` only after all installments are paid.
- **FR-5:** Owned dashboard, bounded Spring AI product recommendations, owned policy search, and complete FE-1 absence.

### Non-functional requirements

- Java 21, Spring Boot 3.5.7, Spring Web, Security, Data JPA, PostgreSQL, Spring AI, Bean Validation, React.js, Axios.
- WCAG 2.2 AA target, responsive 320 px reflow, keyboard/focus/live-region behavior.
- No floating-point money; PostgreSQL `NUMERIC(19,2)`, scale 2, `HALF_UP`.
- Stateless JWT; no sensitive logging; safe error envelope; database and service constraints.
- Append-only `premium_payment` and `audit_event`.
- AWS production topology: ALB, ECS Fargate, RDS PostgreSQL Multi-AZ, CloudWatch, S3/CloudFront.

### Architecture requirements

- Modular layered monolith; controllers do not invoke repositories.
- Exactly seven tables and no claim table/entity/API/DTO/story.
- REST under `/api/v1/**` with DTOs, ISO dates, stable API errors.
- Spring AI is downstream of deterministic eligibility filtering.
- Policy registration and payment mutations are transactional and concurrency-safe.

### UX requirements

- Seven surfaces: Login, Dashboard, Product Discovery, AI Recommendations, Policy Registration, Premium Payment, Policy Search.
- Shared shell, status components, money summary, reusable forms, cards/tables, empty/error/loading states.
- Focus restoration, field error association, non-color status, reduced motion, 44 px touch targets.

## 2. Epic list and dependency order

1. **Epic 1 — Secure customer access**: A customer registers, signs in with JWT, maintains permitted profile data, and cannot access another customer's resources.
2. **Epic 2 — Owned customer dashboard**: A customer sees only their policy/payment summary, amount due, next due, and actionable states.
3. **Epic 3 — Eligible product discovery and guidance**: A customer discovers active, age-eligible, not-held products and receives bounded non-binding Spring AI guidance; an administrator maintains products.
4. **Epic 4 — Policy registration and schedules**: A customer registers one eligible policy per product and receives a financial snapshot plus correct installment schedule/maturity.
5. **Epic 5 — Premium payment and immutable history**: A customer confirms an immediate simulated full/partial payment with correct grace/penalty and immutable `TXN-HD-XXXXXX` evidence.
6. **Epic 6 — Owned policy search**: A customer filters and opens only policies owned by the JWT principal.
7. **Epic 7 — Production readiness and assessment verification**: The full stack is deployable to the approved AWS topology and verified for required scope, security, accessibility, and FE-1 absence.

Natural dependency: Epic 1 precedes all customer-owned epics. Epic 4 precedes payment. Epic 2 can land with available policy data and degrade cleanly to empty state. Epic 7 validates the integrated result.

## 3. FR coverage map

| Requirement | Coverage |
|---|---|
| FR-1 | US-01 |
| FR-2 | US-03, US-04 |
| FR-3 | US-04 |
| FR-4 | US-05 |
| FR-5 | US-02, US-03, US-06 |
| Security/NFR | US-01 through US-07 |
| UX/accessibility | US-01 through US-06 |
| AWS/operations | US-07 |
| FE-1 exclusion | Every story; explicit US-03 and US-07 negative checks |

## 4. Stories and acceptance criteria

### US-01 — Register, authenticate, and isolate customer access

**Epic:** 1  
**As a** customer,  
**I want** to register and sign in with secure JWT authentication,  
**So that** I can use only my own HealthDekho data.

**Acceptance criteria**

- **AUTH-01:** `POST /api/v1/auth/register` validates name, unique normalized email, phone, date of birth, address, and a nonblank password.
- **AUTH-02:** The persisted password is BCrypt-hashed; response, logs, audit metadata, and errors never contain the password/hash/token.
- **AUTH-03:** Duplicate email returns a deterministic conflict and creates no second `customer`.
- **AUTH-04:** `POST /api/v1/auth/login` verifies credentials through Spring Security and returns a signed JWT.
- **AUTH-05:** Invalid email/password returns the same safe authentication response without account enumeration.
- **AUTH-06:** Protected `/api/v1/**` requests without a valid, unexpired JWT return 401.
- **AUTH-07:** Customer identity always comes from JWT claims and the database principal; request `customerId` cannot override it.
- **AUTH-08:** A customer receives 403/neutral 404 for foreign-owned policy, schedule, payment, recommendation, or search resources.
- **AUTH-09:** React Login preserves valid input on non-credential errors, focuses the first invalid field, and exposes no password by default.
- **AUTH-10:** No stateful HTTP session is required for authenticated API behavior.

**Definition of done:** registration/login API and UI tests pass, security-negative cases pass, and no sensitive values appear in logs.

### US-02 — View an owned dashboard

**Epic:** 2  
**As a** customer,  
**I want** an at-a-glance dashboard of my policies and premiums,  
**So that** I can understand obligations and choose the next action safely.

**Acceptance criteria**

- **DASH-01:** `GET /api/v1/dashboard` derives customer ownership from JWT and never aggregates another customer's records.
- **DASH-02:** Dashboard returns owned policy count/status summaries, total current amount due, next due date, and recent immutable payment rows.
- **DASH-03:** Amounts use `BigDecimal`-derived decimal values and include penalty separately from scheduled premium.
- **DASH-04:** Empty dashboard renders “No policies yet” with a Discover Products action.
- **DASH-05:** Partial loading errors retain the authenticated shell and offer section-level retry where safe.
- **DASH-06:** Summary cards and policy cards provide text status, keyboard-accessible actions, and responsive one/two/three-column layouts.
- **DASH-07:** A `401` clears the JWT and routes to Login with a session-expired message; `403` reveals no foreign object details.
- **DASH-08:** Skeleton and busy states do not shift controls and expose accessible `aria-busy` semantics.

**Definition of done:** dashboard contract, ownership tests, empty/error states, and responsive/accessibility checks pass.

### US-03 — Discover eligible products and receive bounded AI guidance

**Epic:** 3  
**As a** customer,  
**I want** to discover products that I can actually register and receive optional non-binding guidance,  
**So that** I can compare valid options without AI overriding eligibility.

**Acceptance criteria**

- **DISC-01:** Admin-only product create/update/deactivate APIs validate unique code, frequency, monetary/range constraints, and the mandatory stack model.
- **DISC-02:** Product discovery returns only `active = true` products not already registered by the JWT customer and satisfying inclusive `min_entry_age <= age <= max_entry_age`.
- **DISC-03:** Deactivation removes a product from new discovery without mutating existing policy snapshots.
- **DISC-04:** Discovery has responsive product cards, empty state, safe retry, and accessible detail/register actions.
- **AI-01:** `POST /api/v1/recommendations` first filters candidates deterministically; Spring AI receives no ineligible, inactive, or already-held product.
- **AI-02:** AI output may rank/explain candidates only, is validated against candidate IDs, and cannot introduce a new product.
- **AI-03:** Each recommendation is labelled non-binding product guidance and stores provenance in `ai_recommendation` without claims fields.
- **AI-04:** AI output is escaped as plain text in React and cannot execute HTML/script.
- **AI-05:** AI provider failure falls back to deterministic eligible products with “AI explanation unavailable” copy.
- **AI-06:** No claim prompt, route, DTO, entity, table, screen, or acceptance behavior exists.

**Definition of done:** product/eligibility tests and bounded-AI tests pass, including malicious output and provider-failure cases.

### US-04 — Register an eligible policy and generate its schedule

**Epic:** 4  
**As a** customer,  
**I want** to register one eligible product with transparent terms,  
**So that** my policy and future payment schedule are exact and stable.

**Acceptance criteria**

- **REG-01:** `POST /api/v1/policies` accepts product, start date, and term; customer identity comes only from JWT.
- **REG-02:** The product must be active and customer age at start date must be within its inclusive product-specific range.
- **REG-03:** `termMonths` must be within the selected product's inclusive range.
- **REG-04:** Database and service enforce `UNIQUE(customer_id, product_id)`; races map to `POLICY_ALREADY_EXISTS` without a second row.
- **REG-05:** Policy snapshots premium, coverage, frequency, term, and eligibility evidence used at registration.
- **REG-06:** `maturity_date = start_date.plusMonths(term_months)`.
- **REG-07:** Frequency generates schedules at +1, +3, or +12 calendar months, through the term; first due date follows the assessment-approved schedule sequence from start date.
- **REG-08:** Each schedule gets `grace_period_end_date = due_date.plusDays(15)`.
- **REG-09:** Policy starts `ACTIVE`; permitted policy states are `ACTIVE`, `PAID`, `MATURED`, `CANCELLED` only.
- **REG-10:** React review repeats product, start, term, maturity, premium, coverage, and issue consequence before confirmation.
- **REG-11:** Duplicate or validation errors preserve valid form values and do not generate partial policy/schedule rows.
- **REG-12:** Policy registration, schedules, and audit are committed in one transaction.

**Definition of done:** product eligibility, date/frequency boundaries, duplicate race, snapshot, transaction rollback, and responsive review tests pass.

### US-05 — Pay a premium fully or partially and preserve the ledger

**Epic:** 5  
**As a** customer,  
**I want** to make an immediately confirmed simulated premium payment,  
**So that** balances, penalties, and audit history remain correct and immutable.

**Acceptance criteria**

- **PAY-01:** Payment endpoint accepts an amount only for a schedule belonging to a JWT-owned `ACTIVE` policy.
- **PAY-02:** On/before grace end, penalty is `0.00`; after grace end, penalty is `scheduled_amount * 0.10` rounded to scale 2 with `HALF_UP` and assessed for that schedule.
- **PAY-03:** Penalty is stored in `premium_payment.penalty_amount` and shown separately from scheduled premium.
- **PAY-04:** Payment below remaining balance plus applicable penalty succeeds as `PARTIALLY_PAID` and leaves the exact positive balance.
- **PAY-05:** Payment greater than remaining balance plus applicable penalty is rejected without schedule, policy, or ledger mutation.
- **PAY-06:** A full settlement sets the schedule `PAID` only when remaining balance is zero.
- **PAY-07:** `customer_policy.status` becomes `PAID` only after every schedule is `PAID`.
- **PAY-08:** Every successful simulated payment immediately appends one immutable `premium_payment` row with unique `TXN-HD-XXXXXX` and `CONFIRMED` state.
- **PAY-09:** Application/database controls provide no ordinary update or delete path for payment rows; corrections append records.
- **PAY-10:** Payment update and ledger append are one concurrency-safe transaction with duplicate-submit protection.
- **PAY-11:** Payment UI shows due, grace end, scheduled, already paid, penalty, remaining, entered amount, and confirmation reference.
- **PAY-12:** Network ambiguity is not presented as a definitive failure before reference/ledger reconciliation.

**Definition of done:** day 15/day 16, partial, full, penalty, overpayment, concurrent duplicate, policy-PAID, and immutable-ledger tests pass.

### US-06 — Search and inspect owned policies

**Epic:** 6  
**As a** customer,  
**I want** to search and open my policies,  
**So that** I can find a policy and its payment state without seeing another customer's data.

**Acceptance criteria**

- **SEA-01:** `GET /api/v1/policies` always includes authenticated customer ID in server-side filtering.
- **SEA-02:** Supported filters include text, policy status, product, start-date range, paging, and bounded sorting.
- **SEA-03:** A customer cannot submit a customer identifier filter to select another customer.
- **SEA-04:** Status filter accepts only `ACTIVE`, `PAID`, `MATURED`, `CANCELLED` and returns safe validation on other values.
- **SEA-05:** `GET /api/v1/policies/{policyId}` returns only an owned detail or ownership-neutral denial/not-found response.
- **SEA-06:** UI uses table semantics on wide screens and accessible cards on small screens.
- **SEA-07:** No-result state explains applied filters and exposes Clear Filters; zero-policy state links to Discover Products.
- **SEA-08:** Search/deep-link input is encoded and not interpolated into SQL or rendered as HTML.
- **SEA-09:** Long names/numbers wrap and 200% zoom does not remove filters or actions.

**Definition of done:** query, ownership/IDOR, paging/sort bounds, empty/filter, injection-resistance, and responsive behavior tests pass.

### US-07 — Verify deployability, operations, and scope fidelity

**Epic:** 7  
**As an** assessment evaluator and operator,  
**I want** the integrated system to deploy and produce verifiable evidence,  
**So that** the capstone can be evaluated consistently in a production-like AWS design.

**Acceptance criteria**

- **OPS-01:** Runtime is Java 21 and Spring Boot 3.5.7 with all mandatory Spring/React/Axios technologies configured and documented.
- **OPS-02:** PostgreSQL migrations create exactly seven approved tables and the required keys/checks/indexes; no claim tables exist.
- **OPS-03:** Automated verification covers US-01 through US-06, including negative authorization and business-rule boundaries.
- **OPS-04:** ALB routes `/api/v1/**` to multi-AZ ECS Fargate; RDS PostgreSQL is Multi-AZ and private; CloudWatch receives structured logs/metrics.
- **OPS-05:** S3/CloudFront serves React assets; Secrets Manager supplies database/AI secrets through least-privilege roles.
- **OPS-06:** Logs contain correlation IDs and safe metadata but no JWTs, passwords, hashes, or sensitive seed credentials.
- **OPS-07:** FE-1 negative test confirms zero claim routes/entities/tables/navigation/prompts/stories/artifacts.
- **OPS-08:** Health/readiness behavior permits ALB and ECS orchestration without exposing secret or database internals.
- **OPS-09:** CI/build/test checks fail on forbidden float/double money, prohibited claim artifacts, or missing mandatory dependencies.
- **OPS-10:** Implementation readiness checklist is PASS before code generation; human approval is recorded in sprint status.

**Definition of done:** local reproducible build/test evidence and reviewed AWS deployment/runbook artifacts pass the implementation readiness gate.

## 5. Implementation readiness checklist

| Check | Required evidence | Gate state |
|---|---|---|
| Approved assessment baseline | `docs/assessment-requirements.md` status and decisions | PASS |
| Product/PRD approved | planning artifacts reconciled | PASS |
| UX flows/screens/state/accessibility | `_bmad-output/ux-design-specification.md` | PASS |
| Architecture/C4/ADR/API/ER/AWS | `_bmad-output/architecture.md` and `docs/architecture.md` | PASS-PENDING-HUMAN-APPROVAL |
| FR coverage | FR-1–FR-5 map to US-01–US-06 | PASS |
| FE-1 excluded | no claim tables/routes/screens/stories | PASS |
| Mandatory stack | all required technologies preserved | PASS |
| Database | seven-table contract and constraints complete | PASS |
| Security | JWT, BCrypt, ownership model complete | PASS |
| Financial rules | 15-day, 10%, partial, overpayment, BigDecimal complete | PASS |
| Application code | must not exist before architecture approval | BLOCKED-UNTIL-APPROVAL |
| Human architecture approval | explicit approval after artifact review | PENDING |

## 6. Delivery sequence

1. US-01 — establish secure customer boundary.
2. US-04 — establish policy/schedule model and registration (with temporary hidden/minimal API proof as required by vertical slicing).
3. US-03 — add product discovery and bounded recommendations.
4. US-02 — expose dashboard aggregates.
5. US-05 — implement payment lifecycle after schedules exist.
6. US-06 — expose owned policy search.
7. US-07 — production-readiness and integrated assessment verification.

This sequence is story planning only. **Do not create application code until human approval of Activity 3/4 artifacts.**
