-- Migration to add portfolio type and user id
ALTER TABLE portfolios ADD COLUMN user_id BIGINT;
ALTER TABLE portfolios ADD COLUMN type VARCHAR(20);

-- Update existing data
UPDATE portfolios SET user_id = 1, type = 'AUTO' WHERE name = 'AI Auto Trader';

-- Make columns NOT NULL after updating existing data
ALTER TABLE portfolios ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE portfolios ALTER COLUMN type SET NOT NULL;

-- Add unique constraint
ALTER TABLE portfolios ADD CONSTRAINT uk_portfolio_user_type UNIQUE (user_id, type);

-- Initialize Manual Portfolio for user 1
INSERT INTO portfolios (name, user_id, type, initial_capital, cash_balance, created_at, updated_at)
VALUES ('Manual Trading Test', 1, 'MANUAL', 100000000.0000, 100000000.0000, NOW(), NOW())
ON CONFLICT DO NOTHING;
