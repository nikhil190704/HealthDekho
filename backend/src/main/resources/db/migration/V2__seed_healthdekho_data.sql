INSERT INTO customer (full_name, email, password_hash, date_of_birth, phone_number, city)
VALUES
  ('Rahul Sharma', 'rahul.sharma@healthdekho.com', '$2a$10$ZVe80sh+DOcU3KTR/AIkIuRvVEz5J65hZtiCAPnvuBmLTxUf.yJco', '1992-05-14', '+91-9876543210', 'Gurugram'),
  ('Priya Patel', 'priya.patel@healthdekho.com', '$2a$10$ZVe80sh+DOcU3KTR/AIkIuRvVEz5J65hZtiCAPnvuBmLTxUf.yJco', '1988-11-22', '+91-9811122233', 'Bengaluru');

INSERT INTO insurance_product (product_code, product_name, policy_type, description, term_months, base_premium_amount, premium_frequency, sum_insured, min_entry_age, max_entry_age, active)
VALUES
  ('HD-HLTH-101', 'HealthDekho Care Shield Plus', 'HEALTH', 'Comprehensive individual hospitalization cover with cashless network hospitals, pre/post hospitalization, and annual wellness checkups.', 12, 12500.00, 'YEARLY', 500000.00, 18, 65, TRUE),
  ('HD-FMLY-201', 'HealthDekho Parivar Floater 360', 'FAMILY_FLOATER', 'Complete family health protection covering self, spouse, and children with zero room-rent capping and maternity benefits.', 24, 9500.00, 'QUARTERLY', 1000000.00, 21, 60, TRUE),
  ('HD-TERM-301', 'HealthDekho Jeevan Suraksha Term', 'TERM_LIFE', 'High-cover pure term life protection plan with accidental death benefit and tax-saving advantages for long-term family security.', 60, 2400.00, 'MONTHLY', 5000000.00, 18, 55, TRUE),
  ('HD-CRIT-401', 'HealthDekho Critical Care Protect', 'CRITICAL_ILLNESS', 'Lump-sum payout on diagnosis of 36 major critical illnesses including cardiac, oncology, and neurological conditions.', 36, 6800.00, 'QUARTERLY', 1500000.00, 25, 65, TRUE),
  ('HD-SENR-501', 'HealthDekho Senior Citizen Care', 'HEALTH', 'Specialized health insurance tailored for senior citizens with domiciliary treatment and daycare procedure cover.', 12, 18500.00, 'YEARLY', 750000.00, 55, 80, TRUE),
  ('HD-ARCH-999', 'HealthDekho Legacy Basic Plan (Retired)', 'HEALTH', 'Discontinued legacy base health plan.', 12, 8000.00, 'YEARLY', 300000.00, 18, 60, FALSE);

INSERT INTO customer_policy (policy_number, customer_id, product_id, start_date, maturity_date, premium_frequency, premium_amount, total_premium_payable, sum_insured, policy_status)
VALUES
  ('POL-HD-2026-1001', 1, 1, '2026-01-15', '2027-01-15', 'YEARLY', 12500.00, 12500.00, 500000.00, 'PAID'),
  ('POL-HD-2026-1002', 1, 2, '2026-06-01', '2028-06-01', 'QUARTERLY', 9500.00, 76000.00, 1000000.00, 'ACTIVE'),
  ('POL-HD-2026-2001', 2, 3, '2026-08-01', '2031-08-01', 'MONTHLY', 2400.00, 144000.00, 5000000.00, 'ACTIVE');

INSERT INTO premium_installment (policy_id, installment_number, due_date, grace_period_end_date, scheduled_amount, paid_amount, penalty_amount, installment_status, paid_at)
VALUES
  (1, 1, '2026-01-15', '2026-01-30', 12500.00, 12500.00, 0.00, 'PAID', '2026-01-14 10:30:00'),
  (2, 1, '2026-06-01', '2026-06-16', 9500.00, 9500.00, 0.00, 'PAID', '2026-06-01 11:15:00'),
  (2, 2, '2026-09-01', '2026-09-16', 9500.00, 0.00, 0.00, 'OVERDUE', NULL),
  (2, 3, '2026-12-01', '2026-12-16', 9500.00, 0.00, 0.00, 'PENDING', NULL),
  (2, 4, '2027-03-01', '2027-03-16', 9500.00, 0.00, 0.00, 'PENDING', NULL),
  (2, 5, '2027-06-01', '2027-06-16', 9500.00, 0.00, 0.00, 'PENDING', NULL),
  (2, 6, '2027-09-01', '2027-09-16', 9500.00, 0.00, 0.00, 'PENDING', NULL),
  (2, 7, '2027-12-01', '2027-12-16', 9500.00, 0.00, 0.00, 'PENDING', NULL),
  (2, 8, '2028-03-01', '2028-03-16', 9500.00, 0.00, 0.00, 'PENDING', NULL),
  (3, 1, '2026-08-01', '2026-08-16', 2400.00, 2400.00, 0.00, 'PAID', '2026-08-01 09:00:00'),
  (3, 2, '2026-09-01', '2026-09-16', 2400.00, 1200.00, 0.00, 'PARTIALLY_PAID', NULL),
  (3, 3, '2026-12-01', '2026-12-16', 2400.00, 0.00, 0.00, 'PENDING', NULL);

INSERT INTO premium_payment (transaction_reference, policy_id, installment_id, customer_id, payment_date, amount_paid, penalty_amount, total_charged, payment_mode, payment_status)
VALUES
  ('TXN-HD-100001', 1, 1, 1, '2026-01-14', 12500.00, 0.00, 12500.00, 'UPI', 'SUCCESS'),
  ('TXN-HD-100002', 2, 1, 1, '2026-06-01', 9500.00, 0.00, 9500.00, 'NET_BANKING', 'SUCCESS'),
  ('TXN-HD-200001', 3, 1, 2, '2026-08-01', 2400.00, 0.00, 2400.00, 'DEBIT_CARD', 'SUCCESS'),
  ('TXN-HD-200002', 3, 2, 2, '2026-09-10', 1200.00, 0.00, 1200.00, 'UPI', 'SUCCESS');
