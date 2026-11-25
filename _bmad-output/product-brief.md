---
title: HealthDekho - Insurance Management System
status: approved
created: 2026-11-25
updated: 2026-11-25
source: docs/assessment-requirements.md
---

# Product Brief: HealthDekho - Insurance Management System

## 1. Product purpose

HealthDekho is a greenfield, full-stack insurance management system built for a Java FDE Capstone Assessment. It gives customers a secure JWT-authenticated way to view their dashboard, discover eligible products, receive bounded Spring AI product guidance, register one policy per product, make full or partial premium payments, view immutable transaction history, and search owned policies. Administrators maintain the product catalog.

The application is intentionally assessment-sized: it demonstrates enterprise fundamentals—authentication, authorization, data integrity, transactional financial processing, validation, auditing, and a responsive REST-powered UI—without adding unrelated insurance or AI features.

## 2. Problem and opportunity

Insurance workflows require accurate eligibility, financial calculations, ownership enforcement, and traceable status changes. A fragmented prototype that trusts client input, uses floating-point arithmetic, or mutates payment history would not demonstrate production-minded Java engineering.

HealthDekho addresses this by making domain rules explicit and enforcing them across API validation, service logic, and seven PostgreSQL tables. The assessment can then verify not only UI/API behavior but also the integrity of the underlying data model.

## 3. Target users and jobs to be done

### Customer

- Register and securely sign in.
- Discover active products and understand their age and term constraints.
- Purchase an eligible policy without a duplicate product policy.
- Track maturity, premium obligations, remaining balances, and immutable payment history.
- Submit and track a claim for an owned policy.

### Administrator

- Maintain the product catalog and its product-specific eligibility rules.
- Inspect customer policies and payment records for support and assessment purposes.
- Manually review claims and preserve every status transition.

### Assessment evaluator

- Verify end-to-end FR-1 through FR-5.
- Inspect technology usage, database constraints, security, financial precision, history immutability, and explicit exclusion of AI claim assessment.

## 4. Proposed product experience

1. A customer registers and signs in through a React interface backed by Spring Security.
2. The customer browses active products and selects an eligible product.
3. Before policy creation, HealthDekho validates customer age, product term, product activity, and duplicate `(customer_id, product_id)` protection.
4. The issued policy displays the premium, coverage, start date, calculated maturity date, and status.
5. Premiums may be paid fully or partially. The customer sees the due amount, grace status, 10% late fee when applicable, and resulting balance.
6. Every payment result is appended to immutable transaction history.
7. The customer can search owned policies and view recommendations for active, age-matched, not-held products. AI output is non-binding product guidance only; no claim workflow exists.

## 5. Current scope

### In scope

- FR-1: Customer Registration and Authentication
- FR-2: Insurance Product Management and Discovery
- FR-3: Policy Purchase and Lifecycle Management
- FR-4: Premium Payment and Transaction History
- FR-5: Dashboard, AI Product Recommendations, and Policy Search
- Seven-table PostgreSQL domain model and development seed data
- React.js UI using Axios

### Explicit non-goals

- FE-1: AI-Assisted Claim Assessment and Approval
- Any claim entity, DTO, endpoint, table, screen, or story
- Real payment-gateway processing
- Underwriting beyond the specified age, term, policy, and coverage rules
- SSO, messaging, document storage, mobile-native apps, analytics dashboards, and unrelated insurance capabilities

## 6. Mandatory product constraints

- Java 21 and Spring Boot 3.5.7
- Spring Web, Spring Security, Spring Data JPA, PostgreSQL, Spring AI, and Bean Validation
- React.js and Axios
- `BigDecimal` for all monetary and rate calculations; PostgreSQL `NUMERIC` for persistence
- BCrypt password hashing
- Duplicate `(customer_id, product_id)` protection in service logic and PostgreSQL
- Product-specific inclusive `min_entry_age` and `max_entry_age`
- Maturity as `start_date.plusMonths(term_months)`
- 15-day grace period and a 10% post-grace payment penalty
- Append-only premium payment and audit histories

Spring AI ranks or explains only candidates already validated as active, not-held, and age-eligible. It has no claim workflow.

## 7. Core business outcomes

The capstone will be considered valuable when an evaluator can demonstrate:

- secure customer and administrator journeys through FR-1–FR-5;
- deterministic eligibility and date calculations;
- financially accurate, concurrency-safe premium payments;
- customer ownership enforcement across dashboards, products, policies, payments, recommendations, and search;
- tamper-resistant transaction history; and
- strict absence of FE-1 functionality.

## 8. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Financial precision errors | Use `BigDecimal`, fixed two-decimal scale, explicit `HALF_UP` rounding, database `NUMERIC`, and boundary tests. |
| Duplicate policy races | Enforce a unique database constraint and translate the conflict to a safe API error. |
| Client-supplied ownership spoofing | Resolve customer ownership from the authenticated principal and authorize every resource. |
| History tampering | Expose append-only APIs and restrict UPDATE/DELETE operations and database privileges. |
| Premature product edits changing policies | Snapshot policy premium, coverage, term, and eligibility evidence at purchase. |
| AI scope creep | Keep FE-1 in the explicit non-goals and acceptance checks. |
| Unclear payment lifecycle | Resolve schedule generation and late-fee capitalization before architecture approval. |

## 9. Resolved decisions and remaining configuration choices

OQ-1 through OQ-6 are resolved exactly as documented in `docs/assessment-requirements.md`: JWT stateless auth, four policy states, frequency schedules, 15-day grace, 10% penalty on scheduled amount, partial/overpayment behavior, immediate immutable simulation, and bounded eligible product AI. FE-1 and all claim management are excluded.

Remaining configuration choices are the AI provider/model, exact JWT expiry/key rotation, currency display, environment values, test threshold, and operational sizing. None permit claims or substitute the mandatory stack.
