CREATE TABLE customer (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    date_of_birth DATE NOT NULL,
    phone VARCHAR(32),
    role VARCHAR(32) NOT NULL DEFAULT 'CUSTOMER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_customer_role CHECK (role = 'CUSTOMER')
);
CREATE TABLE insurance_product (
    id BIGSERIAL PRIMARY KEY,
    product_code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(40) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    base_annual_premium NUMERIC(19,2) NOT NULL CHECK (base_annual_premium > 0),
    min_age INTEGER NOT NULL CHECK (min_age >= 0),
    max_age INTEGER NOT NULL,
    min_term_years INTEGER NOT NULL CHECK (min_term_years > 0),
    max_term_years INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_product_age CHECK (max_age >= min_age),
    CONSTRAINT ck_product_term CHECK (max_term_years >= min_term_years)
);
CREATE TABLE customer_policy (
    id BIGSERIAL PRIMARY KEY,
    policy_number VARCHAR(30) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    product_id BIGINT NOT NULL REFERENCES insurance_product(id),
    start_date DATE NOT NULL,
    maturity_date DATE NOT NULL,
    term_years INTEGER NOT NULL CHECK (term_years > 0),
    annual_premium NUMERIC(19,2) NOT NULL CHECK (annual_premium > 0),
    total_premium NUMERIC(19,2) NOT NULL CHECK (total_premium > 0),
    frequency VARCHAR(20) NOT NULL,
    state VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customer_product UNIQUE (customer_id, product_id),
    CONSTRAINT ck_policy_dates CHECK (maturity_date >= start_date),
    CONSTRAINT ck_policy_state CHECK (state IN ('ACTIVE','PAID','MATURED','CANCELLED')),
    CONSTRAINT ck_policy_frequency CHECK (frequency IN ('MONTHLY','QUARTERLY','HALF_YEARLY','YEARLY'))
);
CREATE TABLE premium_installment (
    id BIGSERIAL PRIMARY KEY,
    policy_id BIGINT NOT NULL REFERENCES customer_policy(id),
    installment_number INTEGER NOT NULL CHECK (installment_number > 0),
    due_date DATE NOT NULL,
    grace_period_end DATE NOT NULL,
    scheduled_amount NUMERIC(19,2) NOT NULL CHECK (scheduled_amount > 0),
    late_fee NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (late_fee >= 0),
    paid_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    state VARCHAR(24) NOT NULL,
    paid_at TIMESTAMP,
    CONSTRAINT uq_policy_installment UNIQUE (policy_id, installment_number),
    CONSTRAINT ck_grace_date CHECK (grace_period_end = due_date + 15),
    CONSTRAINT ck_installment_state CHECK (state IN ('PENDING','PAID','PARTIALLY_PAID','OVERDUE'))
);
CREATE TABLE premium_payment (
    id BIGSERIAL PRIMARY KEY,
    installment_id BIGINT NOT NULL REFERENCES premium_installment(id),
    transaction_reference VARCHAR(100) NOT NULL UNIQUE,
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    state VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_payment_state CHECK (state = 'CONFIRMED')
);
CREATE TABLE ai_recommendation_request (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    request_text VARCHAR(2000) NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_recommendation_outcome CHECK (outcome IN ('AI','FALLBACK'))
);
CREATE TABLE ai_recommendation_item (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL REFERENCES ai_recommendation_request(id),
    product_id BIGINT NOT NULL REFERENCES insurance_product(id),
    reason VARCHAR(1000) NOT NULL,
    rank INTEGER NOT NULL CHECK (rank > 0),
    CONSTRAINT uq_recommendation_product UNIQUE (request_id, product_id),
    CONSTRAINT uq_recommendation_rank UNIQUE (request_id, rank)
);
CREATE INDEX ix_customer_policy_customer ON customer_policy(customer_id);
CREATE INDEX ix_customer_policy_product ON customer_policy(product_id);
CREATE INDEX ix_installment_policy_due ON premium_installment(policy_id, due_date);
CREATE INDEX ix_payment_installment ON premium_payment(installment_id);
CREATE INDEX ix_ai_request_customer_created ON ai_recommendation_request(customer_id, created_at);
CREATE OR REPLACE FUNCTION prevent_premium_payment_mutation() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'premium payments are immutable'; END; $$;
CREATE TRIGGER trg_prevent_premium_payment_update BEFORE UPDATE OR DELETE ON premium_payment FOR EACH ROW EXECUTE FUNCTION prevent_premium_payment_mutation();
