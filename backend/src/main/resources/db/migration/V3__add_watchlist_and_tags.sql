-- Add Watchlist table
CREATE TABLE watchlist (
                           id BIGSERIAL PRIMARY KEY,
                           ticker VARCHAR(20) NOT NULL UNIQUE,
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_watchlist_ticker ON watchlist(ticker);

-- Add tags to NewsArticle
ALTER TABLE news_articles ADD COLUMN tags VARCHAR(255);
