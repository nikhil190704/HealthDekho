---
title: HealthDekho - Insurance Management System
status: approved
created: 2026-11-25
updated: 2026-11-25
source: docs/assessment-requirements.md
brief: _bmad-output/product-brief.md
scope: FR-1 through FR-5
excluded: FE-1
---

# PRD: HealthDekho - Insurance Management System

## 0. Document purpose

This PRD converts the capstone assessment baseline into product behavior, stable functional requirements, acceptance criteria, constraints, business rules, and data requirements for downstream UX, architecture, epics, and implementation. Its source of truth is `docs/assessment-requirements.md`; the Activity 1 analysis is `_bmad-output/product-brief.md`.

FR identifiers are stable and map directly to assessment requirements FR-1 through FR-5. OQ-1 through OQ-6 were approved on 2026-11-25 and are implemented as planning constraints below.

## 1. Vision

HealthDekho gives customers a secure JWT-authenticated web channel for an owned dashboard, eligible product discovery, bounded non-claim AI product guidance, policy registration, premium payments, and policy search. Administrators maintain products. Financial integrity, customer ownership, duplicate prevention, and immutable history are first-class product behavior.

The system is a greenfield Java 21/Spring Boot assessment application with a React.js client. It demonstrates enterprise-level layering and safeguards while deliberately avoiding unrelated features. **FE-1 AI-Assisted Claim Assessment and Approval is not part of the product.**

## 2. Target users

### 2.1 Jobs to be done

- **Customers:** register, authenticate, view owned dashboards, discover eligible products, register policies, track/pay premiums, and search owned policies.
- **Administrators:** configure products and inspect required operational records.
- **Assessment evaluators:** verify security, correctness, scope fidelity, PostgreSQL design, and React/Spring integration.

### 2.2 Non-users for v1

- Unauthenticated users who expect to own or mutate policy data
- External payment processors or insurers
- Automated claim adjudicators or AI agents
- Mobile-native users
- Reporting/analytics users beyond the listed operational journeys

### 2.3 User journeys

- **UJ-1 — Aisha registers and returns securely.** Aisha enters profile details and a password, registers, signs in, and receives a customer workspace. Invalid credentials or duplicate email produce a safe failure with no account disclosure. Aisha can never open another customer's records.
- **UJ-2 — Rohan discovers and purchases a product.** Rohan signs in, views active products, selects one, supplies start date and term, and receives a policy. The product's own age and term limits are enforced; the maturity date and financial values are exact. A second policy for the same product is rejected.
- **UJ-3 — Aisha pays a premium.** Aisha selects an owned policy installment, sees the due date, amount due, grace status, and possible penalty, then records a full or partial payment. The system appends an immutable transaction and returns the remaining balance.
- **UJ-4 — Aisha receives eligible product guidance.** Aisha requests a recommendation. HealthDekho filters active, age-matched, not-held products before Spring AI explains them, clearly labels the result non-binding, and lets Aisha open a valid product.
- **UJ-5 — Aisha finds and reviews an owned policy.** Aisha searches by product/status/date, opens only an owned result, and sees policy, schedule, and payment context. Cross-customer identifiers never expose a result.

## 3. Glossary

- **Customer:** A registered principal with a customer role and identity used for ownership decisions.
- **Administrator:** An authenticated principal allowed to maintain products.
- **Insurance Product:** Configurable catalog definition containing eligibility, premium, coverage, term, and active status.
- **Policy:** A customer's issued contract snapshot for one Insurance Product.
- **Premium Schedule:** One dated obligation belonging to a Policy, with a 15-day grace end date.
- **Premium Payment:** Immutable append-only payment ledger row identified by `TXN-HD-XXXXXX`.
- **AI Recommendation:** Non-binding Spring AI ranking/explanation of an already deterministic eligible product candidate.
- **Grace Period:** The 15 calendar days from and after a due date during which no late fee applies; the grace end is `dueDate.plusDays(15)`.
- **Partial Payment:** A valid payment below remaining amount plus applicable penalty, leaving a positive balance.
- **Available Coverage:** Coverage remaining after applying all approved coverage treatment; its exact aggregation rule remains open.

## 4. Features

### 4.1 Customer registration and authentication

Customers create and use a secure account. The server derives identity from the authenticated principal for all owned operations. Realizes UJ-1.

#### FR-1: Register, authenticate, and authorize customers

The system shall allow a person to register as a Customer, sign in, retrieve/update permitted profile information, and access only authorized resources.

**Consequences (testable):**
- Required profile fields are `name`, `email`, `phone`, `date_of_birth`, `address`, and `password`.
- Email must be valid and unique after defined normalization.
- Passwords are persisted only as BCrypt hashes and never returned or logged.
- Invalid email/password combinations produce a non-enumerating authentication response.
- Anonymous requests to protected endpoints fail.
- Customer and administrator authorization checks are enforced server-side.
- A client-supplied `customer_id` cannot override the authenticated principal's ownership.

**Acceptance criteria:**
- AC-FR1-01: Valid registration returns success and creates exactly one Customer with a BCrypt hash.
- AC-FR1-02: Duplicate email registration fails without creating another Customer.
- AC-FR1-03: Valid credentials authenticate; invalid credentials do not reveal which field failed.
- AC-FR1-04: Customer-owned records cannot be read or changed by another Customer.
- AC-FR1-05: Administrator actions fail for Customer accounts.

**Open questions:** Authentication mechanism and session/token lifetime.

### 4.2 Insurance product management and discovery

Administrators maintain a catalog whose eligibility is configuration per Insurance Product. Customers discover only active products. Realizes UJ-2.

#### FR-2: Maintain and discover insurance products

The system shall support administrator CRUD/deactivation for Insurance Products and customer read-only discovery of active products.

**Consequences (testable):**
- Product code is unique.
- Each product stores `min_entry_age`, `max_entry_age`, `min_term_months`, `max_term_months`, base premium, coverage, frequency, description, and active state.
- Customer discovery excludes inactive products.
- Inactive products remain reachable for historical Policy relationships.
- `min_entry_age <= max_entry_age` and term/minimum/maximum monetary constraints are validated.
- The applicable age range is the selected product's range; no global age rule replaces it.

**Acceptance criteria:**
- AC-FR2-01: Duplicate product code fails at API/service and database level.
- AC-FR2-02: Active catalog results include only active products.
- AC-FR2-03: Historical policies keep resolvable product information after deactivation.
- AC-FR2-04: A product with an invalid range is rejected.
- AC-FR2-05: Product detail exposes public eligibility and pricing information without administrative-only fields.

### 4.3 Policy purchase and lifecycle

An authenticated Customer can purchase at most one Policy for a given Insurance Product. Existing Policy financial values are stable even if catalog configuration changes. Realizes UJ-2.

#### FR-3: Validate and issue policies

The system shall issue a Policy only when the selected active Insurance Product, principal, start date, age, term, and duplicate rules are valid.

**Consequences (testable):**
- Request ownership is derived from the authenticated Customer.
- Eligibility uses the selected product's inclusive age range at `start_date`.
- `term_months` is within the product's inclusive term range.
- A database `UNIQUE (customer_id, product_id)` constraint protects the invariant.
- Service handling provides a deterministic conflict without hiding unrelated database errors.
- `maturity_date = start_date + term_months` using calendar-month semantics.
- Premium, coverage, payment, and recommendation values use `BigDecimal` with explicit precision and scale.
- Policy stores an issuance snapshot of premium and coverage needed for historical consistency.
- A stable unique policy number, issue date, term, and status are persisted.

**Acceptance criteria:**
- AC-FR3-01: Age equal to either inclusive boundary is accepted; one year outside is rejected.
- AC-FR3-02: Term equal to either inclusive boundary is accepted; outside values fail.
- AC-FR3-03: A duplicate customer/product purchase fails and creates no second Policy.
- AC-FR3-04: Maturity date equals the calendar date resulting from adding `term_months`.
- AC-FR3-05: Subsequent product edits do not alter a Policy's stored premium or coverage.
- AC-FR3-06: Floating-point values are not used in domain/API/database money contracts.

**Out of scope:** generalized underwriting, medical risk scoring, quotation/version comparison, and policy renewal beyond the supplied lifecycle definition.

### 4.4 Premium payment and transaction history

Customers can pay current obligations fully or partially while HealthDekho records every result in immutable history. Realizes UJ-3.

#### FR-4: Record premium payments and preserve transaction history

The system shall calculate amounts payable, apply grace and late-fee rules, accept valid full or partial payments, update remaining balances transactionally, and append a Premium Transaction.

**Consequences (testable):**
- A Premium Schedule belongs to one Policy.
- The grace period is 15 calendar days after due date.
- Payment on or before the grace end has no late fee.
- Payment after grace incurs 10% (`BigDecimal("0.10")`) of scheduled amount.
- The penalty is recorded in `premium_payment.penalty_amount`.
- Financial computations use two-decimal scale and `HALF_UP` unless the assessment owner approves a different contract.
- A partial positive payment below the total payable amount is accepted and leaves an exact balance.
- Overpayment is rejected and does not mutate the schedule or create a successful transaction.
- Premium Payment records cannot be updated or deleted through normal application capabilities.
- Corrections create a new transaction/event; they do not rewrite the original transaction.
- Payment/history actions are constrained by principal ownership.

**Acceptance criteria:**
- AC-FR4-01: Payment through `due_date + 15 days` has zero penalty.
- AC-FR4-02: Payment from the next day has exactly 10% of scheduled amount as penalty.
- AC-FR4-03: A `1500.00` scheduled amount paid at `1500.00` after grace leaves `150.00` penalty payable.
- AC-FR4-04: `500.00` partial payment on `1500.00` creates a `1000.00` balance and an immutable record.
- AC-FR4-05: Payment above total payable fails with no successful balance mutation.
- AC-FR4-06: Exposed APIs have no update/delete route for Premium Payment history.

### 4.5 Dashboard, product recommendations, and policy search

Customers use owned operational views and bounded product guidance. FE-1 has no data, API, or UI representation. Realizes UJ-4 and UJ-5.

#### FR-5: Operate the customer workspace

The system shall provide an owned dashboard, deterministic product recommendation candidates, Spring AI product guidance over those candidates, and owned-policy search.

**Consequences (testable):**
- Dashboard totals and next due values query only the authenticated customer.
- Recommendation candidates are active, not already held by the customer, and satisfy product-specific age eligibility.
- Spring AI ranks/explains only the deterministic candidate set and cannot introduce candidates.
- Recommendation data is labelled non-binding and persisted as provenance in `ai_recommendation`.
- Search filters policy/status/product/date only within authenticated ownership.
- No claim model, endpoint, screen, table, or story exists.

**Acceptance criteria:**
- AC-FR5-01: Dashboard excludes every other customer's records.
- AC-FR5-02: Discovery and AI omit inactive, duplicate-held, or age-ineligible products.
- AC-FR5-03: AI unavailability falls back to eligible products without blocking the product workflow.
- AC-FR5-04: Recommendation text cannot become executable HTML and is clearly non-binding.
- AC-FR5-05: Policy search returns only owned policies.
- AC-FR5-06: FE-1 is absent from database, REST paths, frontend navigation, prompts, and stories.

## 5. Non-goals

- FE-1 AI-Assisted Claim Assessment and Approval and all claim management
- Any automated claim decision or AI-generated claim recommendation
- Real payment-gateway integration
- SSO/social login or identity-provider integration
- General underwriting, broker workflows, commissions, renewals, refunds, or cancellations not required above
- Notifications, document upload, analytics, chat, and mobile-native clients
- Replacing PostgreSQL, Spring Boot, React.js, or Axios

## 6. MVP scope

### 6.1 In scope

- FR-1 through FR-5 end-to-end REST and React workflows
- Customer and Administrator roles
- Exactly seven PostgreSQL domain tables:
  1. `customer`
  2. `insurance_product`
  3. `customer_policy`
  4. `premium_schedule`
  5. `premium_payment`
  6. `ai_recommendation`
  7. `audit_event`
- Development seed data from the assessment baseline
- Validation, authorization, uniqueness, transactions, and immutable histories

### 6.2 Out of scope

- FE-1 and every AI-assisted claim assessment behavior
- Features, integrations, or stack substitutions not required by the assessment
- Application code before architecture approval

## 7. Success metrics

### Primary

- **SM-1 — Assessment flow completion:** 100% of automated acceptance cases for FR-1–FR-5 pass. Validates FR-1–FR-5.
- **SM-2 — Rule correctness:** 100% pass for duplicate prevention, age/term boundaries, maturity, grace, late fee, partial payment, ownership, and immutability cases. Validates FR-1–FR-5.
- **SM-3 — Financial determinism:** 100% of monetary test scenarios produce expected two-decimal values with `BigDecimal` and no floating-point storage. Validates FR-3, FR-4, FR-5.
- **SM-4 — Security enforcement:** 100% of unauthorized resource and role test cases fail without data disclosure. Validates FR-1–FR-5.
- **SM-5 — Scope fidelity:** Zero AI-assisted claim behavior and zero mandatory-stack substitutions. Validates FR-5 and all scope constraints.

### Counter-metrics (do not optimize)

- Do not maximize feature count or UI screens at the expense of FR-1–FR-5 correctness.
- Do not optimize for a high raw payment-throughput benchmark when it weakens transaction integrity or auditability.
- Do not reduce acceptance-test count by omitting boundary and security-negative scenarios.

## 8. Cross-cutting non-functional requirements

### 8.1 Security

- Spring Security is required for authentication and authorization.
- Passwords are BCrypt hashes; plaintext and decoded password exposure is prohibited.
- All owned-resource checks occur on the server.
- Validation and authorization failures use safe, deterministic responses that do not expose sensitive existence details.
- Secrets, tokens, credentials, and sensitive values must not be logged.

### 8.2 Data integrity

- PostgreSQL is authoritative.
- Bean Validation, service rules, and database constraints protect critical invariants at multiple layers.
- Policy purchase and premium balance changes are transactional.
- Optimistic or pessimistic controls must protect concurrent policy and payment operations; selection is an architecture decision.
- `premium_payment` and `audit_event` are append-only in exposed application flows; correction is additive.

### 8.3 Financial and date precision

- All monetary values and rates use `BigDecimal`.
- PostgreSQL stores money/rates in `NUMERIC` columns.
- Initial rounding assumption is two decimals with `HALF_UP`; final agreement is required before implementation.
- Maturity is calculated by adding calendar months to `start_date`.
- Dates and grace boundaries are tested explicitly, including month-end behavior.

### 8.4 Maintainability and integration

- The backend uses Java 21, Spring Boot 3.5.7, Spring Web, Spring Security, Spring Data JPA, Spring AI, and Bean Validation.
- The frontend uses React.js and Axios.
- Spring Web provides a versioned REST boundary with DTOs; clients do not bind directly to persistent entities.
- Spring AI's presence must not create FE-1 behavior; provider/configuration scope remains open.

### 8.5 Testability and observability

- Automated tests cover positive, negative, boundary, authorization, and concurrency-relevant acceptance cases for FR-1–FR-5.
- Logs must carry useful event identifiers and errors without credentials or sensitive customer data.
- Assessment evidence must be reproducible from seeded data and documented commands during later setup.

### 8.6 Usability/accessibility

- React forms provide clear field validation and operation results.
- Interactive controls are keyboard accessible.
- No specific WCAG conformance, browser support, or visual system is mandated yet.

### 8.7 Operational requirements

- Availability, RTO/RPO, performance SLAs, deployment target, retention, and support tiers are unspecified.
- Architecture must identify these as constraints or unresolved assumptions rather than inventing enterprise-grade targets.

## 9. Database requirements

The PostgreSQL schema is limited to seven assessment domain tables:

1. **`customer`** — identity, profile, BCrypt `password_hash`, role, timestamps.
2. **`insurance_product`** — product-specific eligibility, frequency, premium/coverage, active state.
3. **`customer_policy`** — unique ownership/product pair, snapshots, dates, term, status, version.
4. **`premium_schedule`** — due/grace dates, scheduled/paid/penalty/remaining values, status.
5. **`premium_payment`** — immutable `TXN-HD-XXXXXX` payment ledger.
6. **`ai_recommendation`** — bounded, non-claim recommendation provenance.
7. **`audit_event`** — append-only safe security/domain event trail.

Critical schema expectations:

- PostgreSQL foreign keys and checks protect references, positive values, ranges, dates, and financial invariants.
- `policies` has `UNIQUE (customer_id, product_id)`.
- `customer_policy` has `UNIQUE(customer_id, product_id)` and policy snapshots.
- `premium_schedule` has one row per unique `(policy_id, installment_number)`.
- `premium_payment` and `audit_event` are append-only through ordinary application behavior.
- All monetary columns use PostgreSQL `NUMERIC(19,2)` or an approved equivalent.

## 10. Constraints and guardrails

- Preserve the mandatory Java/Spring/PostgreSQL/React/Axios stack.
- Do not generate application code before architecture approval.
- Do not add FE-1, claims, claim entities, claim routes, claim tables, or claim UI.
- Client ownership identifiers are untrusted; JWT-derived principal identity wins.
- AI recommendations may rank/explain only deterministic eligible candidates.

## 11. Stakeholders and approvals

- **Assessment owner:** approves scope, behavior, and final architecture.
- **Product owner:** owns approved decisions and remaining configuration choices.
- **Technical architect:** maintains architecture decisions and ADRs.
- **Developer/reviewer:** implements only approved stories and verifies business/security boundaries.

## 12. Risks and mitigations

| ID | Risk | Mitigation |
|---|---|---|
| R-1 | Concurrent payment submissions create duplicate/incorrect balances | Lock/version schedule in one transaction and append one unique ledger reference. |
| R-2 | AI invents an ineligible candidate | Filter candidates deterministically and validate returned product IDs. |
| R-3 | JWT identity is spoofed by a client ID | Derive ownership exclusively from validated principal. |
| R-4 | Product edits mutate historical policies | Snapshot financial/eligibility facts at registration. |
| R-5 | Post-grace fee boundaries are implemented inconsistently | Test grace end and next-day behavior explicitly. |
| R-6 | Scope expands into claims | Negative tests and the FE-1 absence gate apply to DB/API/UI/stories. |

## 13. Acceptance strategy

1. Trace every FR and NFR to US-01 through US-07.
2. Test age, term, maturity, frequency, grace, fee, partial, overpayment, uniqueness, ownership, AI fallback, and immutable-history boundaries.
3. Verify exactly seven approved tables and no claim schema/artifact.
4. Verify mandatory stack and AWS deployment contract before implementation completion.

## 14. Approved decisions

OQ-1 through OQ-6 are resolved and no longer block architecture:

- Stateless JWT authentication with BCrypt login at `POST /api/v1/auth/login` and strict customer isolation.
- Policy states `ACTIVE`, `PAID`, `MATURED`, `CANCELLED`; no claim states or claim management.
- Frequency-generated installments using +1/+3/+12 months, grace end at `dueDate.plusDays(15)`, maturity at `startDate.plusMonths(termMonths)`.
- Mandatory 10% post-grace penalty on scheduled amount, partial payment support, overpayment rejection, and policy `PAID` only after all schedules are paid.
- Immediate simulated confirmation with immutable `premium_payment` rows and unique `TXN-HD-XXXXXX` references.
- Product/AI eligibility limited to active, not-held, age-matched products.

Remaining choices are AI provider/model, exact JWT key-rotation/TTL, currency display, environment values, test threshold, and production sizing.

## 15. Assumptions index

- **A-1:** JWT is carried in the Authorization Bearer header and stored in React memory for the assessment baseline.
- **A-2:** Monetary scale is two decimals with `HALF_UP`.
- **A-3:** The 10% penalty is assessed from the scheduled amount for a post-grace installment and remains part of that schedule's payable balance.
- **A-4:** Spring AI provider failure degrades to deterministic eligible product guidance.
- **A-5:** FE-1 and all claims are absent from the current implementation and data model.

## 16. Approval gate

Activities 1 and 2 are approved. UX, architecture, and stories are complete and awaiting explicit human architecture approval. Application code is prohibited until that approval is recorded.
