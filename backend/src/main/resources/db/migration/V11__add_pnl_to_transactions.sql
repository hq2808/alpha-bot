-- Add PnL columns to portfolio_transactions
ALTER TABLE portfolio_transactions ADD COLUMN cost_price NUMERIC(19, 4);
ALTER TABLE portfolio_transactions ADD COLUMN pnl_value NUMERIC(19, 4);
ALTER TABLE portfolio_transactions ADD COLUMN pnl_percent NUMERIC(19, 4);
