# HealthDekho — Customer Insurance Management System (IMS)

Full-stack Customer Insurance Management Portal built using the **BMAD Method**, **Java 21 + Spring Boot 3.5.7**, **PostgreSQL 18 + Flyway**, **Spring AI**, and **React 18 + Tailwind CSS**.

## Core Features
- **Stateless JWT Authentication (`US-01`):** Customer login and session management with BCrypt password hashing.
- **Policy Catalog & Age-Eligibility Rules (`US-02`):** Browse active insurance plans with automatic entry-age validation (`min_entry_age` – `max_entry_age`).
- **Policy Registration & Installment Schedule Generator (`US-03`):** Enroll in eligible plans (`MONTHLY`, `QUARTERLY`, `YEARLY`) with auto-generated installment schedules and 15-day grace periods.
- **Multi-Criteria Policy Search (`US-04`):** Search registered policies by policy number, customer name, or date of birth.
- **Installment Payment & Late Penalty Engine (`US-05`):** Tracks `PAID`, `PENDING`, and `OVERDUE` installments with automatic 10% late penalty enforcement past the 15-day grace period.
- **Spring AI Personalized Policy Advisor (`US-06`):** Ranks age-eligible plans by suitability score with deterministic fallback support.

## Quick Start
1. **Database:** Ensure PostgreSQL 18 is running with database `healthdekho` (`postgres` / `postgres`).
2. **Backend (`:8080`):** `cd backend && mvn spring-boot:run`
3. **Frontend (`:5173`):** `cd frontend && npm install && npm run dev`
4. **Demo Login:** `rahul.sharma@healthdekho.com` / `Password@123`
  
