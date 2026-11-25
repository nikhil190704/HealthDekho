CREATE TABLE IF NOT EXISTS customer (
    customer_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    city VARCHAR(80),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS insurance_product (
    product_id BIGSERIAL PRIMARY KEY,
    product_code VARCHAR(40) NOT NULL UNIQUE,
    product_name VARCHAR(150) NOT NULL,
    policy_type VARCHAR(40) NOT NULL,
    description TEXT NOT NULL,
    term_months INT NOT NULL CHECK (term_months > 0),
    base_premium_amount NUMERIC(12, 2) NOT NULL CHECK (base_premium_amount > 0),
    premium_frequency VARCHAR(20) NOT NULL CHECK (premium_frequency IN ('MONTHLY', 'QUARTERLY', 'YEARLY')),
    sum_insured NUMERIC(14, 2) NOT NULL CHECK (sum_insured > 0),
    min_entry_age INT NOT NULL CHECK (min_entry_age >= 0),
    max_entry_age INT NOT NULL CHECK (max_entry_age >= min_entry_age),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS customer_policy (
    policy_id BIGSERIAL PRIMARY KEY,
    policy_number VARCHAR(40) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES customer(customer_id) ON DELETE RESTRICT,
    product_id BIGINT NOT NULL REFERENCES insurance_product(product_id) ON DELETE RESTRICT,
    start_date DATE NOT NULL,
    maturity_date DATE NOT NULL,
    premium_frequency VARCHAR(20) NOT NULL CHECK (premium_frequency IN ('MONTHLY', 'QUARTERLY', 'YEARLY')),
    premium_amount NUMERIC(12, 2) NOT NULL CHECK (premium_amount > 0),
    total_premium_payable NUMERIC(14, 2) NOT NULL CHECK (total_premium_payable > 0),
    sum_insured NUMERIC(14, 2) NOT NULL CHECK (sum_insured > 0),
    policy_status VARCHAR(20) NOT NULL CHECK (policy_status IN ('ACTIVE', 'PAID', 'MATURED', 'CANCELLED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customer_product UNIQUE (customer_id, product_id),
    CONSTRAINT chk_policy_dates CHECK (maturity_date > start_date)
);
CREATE TABLE IF NOT EXISTS premium_installment (
    installment_id BIGSERIAL PRIMARY KEY,
    policy_id BIGINT NOT NULL REFERENCES customer_policy(policy_id) ON DELETE CASCADE,
    installment_number INT NOT NULL CHECK (installment_number > 0),
    due_date DATE NOT NULL,
    grace_period_end_date DATE NOT NULL,
    scheduled_amount NUMERIC(12, 2) NOT NULL CHECK (scheduled_amount > 0),
    paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (paid_amount >= 0),
    penalty_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (penalty_amount >= 0),
    installment_status VARCHAR(20) NOT NULL CHECK (installment_status IN ('PENDING', 'PARTIALLY_PAID', 'PAID', 'OVERDUE')),
    paid_at TIMESTAMP NULL,
    CONSTRAINT uq_policy_installment UNIQUE (policy_id, installment_number)
);
CREATE TABLE IF NOT EXISTS premium_payment (
    payment_id BIGSERIAL PRIMARY KEY,
    transaction_reference VARCHAR(40) NOT NULL UNIQUE,
    policy_id BIGINT NOT NULL REFERENCES customer_policy(policy_id) ON DELETE RESTRICT,
    installment_id BIGINT NOT NULL REFERENCES premium_installment(installment_id) ON DELETE RESTRICT,
    customer_id BIGINT NOT NULL REFERENCES customer(customer_id) ON DELETE RESTRICT,
    payment_date DATE NOT NULL,
    amount_paid NUMERIC(12, 2) NOT NULL CHECK (amount_paid > 0),
    penalty_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (penalty_amount >= 0),
    total_charged NUMERIC(12, 2) NOT NULL CHECK (total_charged > 0),
    payment_mode VARCHAR(30) NOT NULL,
    payment_status VARCHAR(20) NOT NULL CHECK (payment_status IN ('SUCCESS', 'FAILED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS ai_recommendation_request (
    recommendation_request_id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(customer_id) ON DELETE CASCADE,
    submitted_dob DATE NOT NULL,
    calculated_age INT NOT NULL,
    insurance_goals TEXT NOT NULL,
    preferred_term_months INT NULL,
    preferred_policy_type VARCHAR(40) NULL,
    ai_source VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS ai_recommendation_item (
    recommendation_item_id BIGSERIAL PRIMARY KEY,
    recommendation_request_id BIGINT NOT NULL REFERENCES ai_recommendation_request(recommendation_request_id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES insurance_product(product_id) ON DELETE RESTRICT,
    rank_order INT NOT NULL CHECK (rank_order > 0),
    suitability_score INT NOT NULL CHECK (suitability_score BETWEEN 1 AND 100),
    suitability_label VARCHAR(30) NOT NULL,
    recommendation_reason TEXT NOT NULL,
    CONSTRAINT uq_recommendation_product UNIQUE (recommendation_request_id, product_id)
);
