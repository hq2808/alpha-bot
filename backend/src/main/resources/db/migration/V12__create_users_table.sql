-- Migration to create users table and link to portfolios
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_provider_id UNIQUE (provider, provider_id)
);

-- Seed initial user to match existing portfolio data (from V10)
INSERT INTO users (id, provider, provider_id, email, created_at)
VALUES (1, 'system', 'system-default', 'system@alphabot.io', NOW())
ON CONFLICT (id) DO NOTHING;

-- Add foreign key constraint to portfolios
ALTER TABLE portfolios 
ADD CONSTRAINT fk_portfolio_user 
FOREIGN KEY (user_id) REFERENCES users(id);
