-- V4: Create rss_feeds table and seed with default working feeds
-- Feeds that have been verified to work from Docker containers

CREATE TABLE IF NOT EXISTS rss_feeds (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    url         VARCHAR(500) NOT NULL UNIQUE,   -- Unique constraint prevents duplicate sources
    category    VARCHAR(50)  DEFAULT 'Global',
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Seed default feeds (ON CONFLICT DO NOTHING = safe re-run)
INSERT INTO rss_feeds (name, url, category, is_active) VALUES
    ('VnExpress',  'https://vnexpress.net/rss/kinh-doanh.rss',                              'VN',     TRUE),
    ('VnEconomy',  'https://vneconomy.vn/chung-khoan.rss',                                 'VN',     TRUE),
    ('Reuters',    'https://feeds.reuters.com/reuters/businessNews',                        'Global', TRUE),
    ('CNBC',       'https://www.cnbc.com/id/100003114/device/rss/rss.html',                'Global', TRUE)
ON CONFLICT (url) DO NOTHING;
