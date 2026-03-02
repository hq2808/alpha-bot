-- V4: Create rss_feeds table and seed with 4 initial default feeds
-- Feeds verified to work from Docker containers

CREATE TABLE IF NOT EXISTS rss_feeds (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    url         VARCHAR(500) NOT NULL UNIQUE,
    category    VARCHAR(50)  DEFAULT 'Global',
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO rss_feeds (name, url, category, is_active) VALUES
    ('VnExpress - Kinh Doanh', 'https://vnexpress.net/rss/kinh-doanh.rss',                    'VN',     TRUE),
    ('VnEconomy - Chứng Khoán', 'https://vneconomy.vn/chung-khoan.rss',                       'VN',     TRUE),
    ('Reuters - Business',      'https://feeds.reuters.com/reuters/businessNews',              'Global', TRUE),
    ('CNBC - Finance',          'https://www.cnbc.com/id/100003114/device/rss/rss.html',      'Global', TRUE)
ON CONFLICT (url) DO NOTHING;
