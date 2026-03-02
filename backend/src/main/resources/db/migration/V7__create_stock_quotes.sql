CREATE TABLE stock_quotes (
    ticker VARCHAR(20) PRIMARY KEY,
    basic_price NUMERIC(15, 2),
    ceiling_price NUMERIC(15, 2),
    floor_price NUMERIC(15, 2),
    
    match_price NUMERIC(15, 2),
    match_qtty NUMERIC(15, 2),
    
    buy_price_1 NUMERIC(15, 2),
    buy_qtty_1 NUMERIC(15, 2),
    buy_price_2 NUMERIC(15, 2),
    buy_qtty_2 NUMERIC(15, 2),
    buy_price_3 NUMERIC(15, 2),
    buy_qtty_3 NUMERIC(15, 2),
    
    sell_price_1 NUMERIC(15, 2),
    sell_qtty_1 NUMERIC(15, 2),
    sell_price_2 NUMERIC(15, 2),
    sell_qtty_2 NUMERIC(15, 2),
    sell_price_3 NUMERIC(15, 2),
    sell_qtty_3 NUMERIC(15, 2),
    
    total_match_qtty NUMERIC(15, 2),
    
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
