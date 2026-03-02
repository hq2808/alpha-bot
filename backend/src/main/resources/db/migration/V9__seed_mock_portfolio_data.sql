-- Seed dummy data to make the Portfolio Panel visually appealing out of the box
DO $$
DECLARE
    v_portfolio_id BIGINT;
    current_equity DECIMAL(19,4) := 100000000.0000;
    iter_date DATE;
BEGIN
    SELECT id INTO v_portfolio_id FROM portfolios WHERE name = 'AI Auto Trader' LIMIT 1;

    IF v_portfolio_id IS NOT NULL THEN
        -- Seed 30 days of snapshots
        FOR i IN 1..30 LOOP
            iter_date := CURRENT_DATE - (30 - i);
            current_equity := current_equity + (random() * 2000000 - 800000); -- Random gain/loss
            
            INSERT INTO portfolio_snapshots (portfolio_id, snapshot_date, total_equity, cash_balance, stock_value)
            VALUES (v_portfolio_id, iter_date, current_equity, current_equity * 0.4, current_equity * 0.6)
            ON CONFLICT (portfolio_id, snapshot_date) DO NOTHING;
        END LOOP;

        -- Seed some positions
        INSERT INTO portfolio_positions (portfolio_id, ticker, quantity, average_price)
        VALUES 
            (v_portfolio_id, 'FPT', 200, 95000.00),
            (v_portfolio_id, 'VCB', 200, 88000.00),
            (v_portfolio_id, 'MWG', 500, 45000.00)
        ON CONFLICT (portfolio_id, ticker) DO NOTHING;

        -- Seed some transactions combining to these positions
        INSERT INTO portfolio_transactions (portfolio_id, ticker, type, quantity, price, total_value, reason, created_at)
        VALUES 
            (v_portfolio_id, 'FPT', 'BUY', 200, 95000.00, 19000000.00, '[AI Conf: 0.85] FPT công bố doanh thu mảng AI tăng trưởng 30%.', NOW() - INTERVAL '3 days'),
            (v_portfolio_id, 'VCB', 'BUY', 200, 88000.00, 17600000.00, '[AI Conf: 0.82] VCB chuẩn bị chia sẻ cổ tức tỷ lệ cao đợt cuối năm.', NOW() - INTERVAL '2 days'),
            (v_portfolio_id, 'MWG', 'BUY', 500, 45000.00, 22500000.00, '[AI Conf: 0.78] Bách Hóa Xanh báo lãi quý thứ 2 liên tiếp, triển vọng sáng.', NOW() - INTERVAL '1 day');
            
        -- Update portfolio cash balance to reflect purchases (initial 100M - 59.1M)
        UPDATE portfolios 
        SET cash_balance = 40900000.00
        WHERE id = v_portfolio_id AND cash_balance = initial_capital;
        
    END IF;
END $$;
