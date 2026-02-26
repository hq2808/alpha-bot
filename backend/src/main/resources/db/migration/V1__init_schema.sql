-- V1__init_schema.sql
-- Initial database schema for Alpha Bot

CREATE TABLE IF NOT EXISTS news_articles (
    id                 BIGSERIAL PRIMARY KEY,
    title              VARCHAR(500) NOT NULL,
    description        VARCHAR(2000),
    url                VARCHAR(1000) NOT NULL UNIQUE,
    source             VARCHAR(100) NOT NULL,
    published_at       TIMESTAMPTZ,
    crawled_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sentiment_score    DOUBLE PRECISION,
    mentioned_tickers  VARCHAR(500),
    ai_summary         VARCHAR(1000),
    is_alert_sent      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_news_crawled_at ON news_articles(crawled_at DESC);
CREATE INDEX idx_news_source ON news_articles(source);
CREATE INDEX idx_news_sentiment ON news_articles(sentiment_score DESC);
CREATE INDEX idx_news_tickers ON news_articles(mentioned_tickers);
