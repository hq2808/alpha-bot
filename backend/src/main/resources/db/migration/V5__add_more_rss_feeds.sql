-- V5: Add more verified working RSS feeds
-- All feeds tested and confirmed valid (HTTP 200 + valid RSS items).
-- ON CONFLICT (url) DO NOTHING = safe to re-run.

-- ── Vietnamese ────────────────────────────────────────────────────────────────
INSERT INTO rss_feeds (name, url, category, is_active) VALUES
    -- 50 items/run — confirmed working
    ('Thanh Niên - Kinh Tế',    'https://thanhnien.vn/rss/kinh-te.rss',                     'VN', TRUE),
    ('Tuổi Trẻ - Kinh Tế',      'https://tuoitre.vn/rss/kinh-te.rss',                       'VN', TRUE),
    -- 50 items/run
    ('Báo Chính Phủ - Kinh Tế', 'https://baochinhphu.vn/kinh-te.rss',                       'VN', TRUE)
ON CONFLICT (url) DO NOTHING;

-- ── Google News RSS — keyword-based, bypasses bot-blocking sites ──────────────
-- Tự tổng hợp từ VnEconomy, Dantri, CafeF, Zing... mà không bị chặn.
-- 100 items/run mỗi feed.
INSERT INTO rss_feeds (name, url, category, is_active) VALUES
    ('Google News - Chứng Khoán VN',
        'https://news.google.com/rss/search?q=chung+khoan+viet+nam&hl=vi&gl=VN&ceid=VN:vi',
        'VN', TRUE),
    ('Google News - Kinh Tế Tài Chính',
        'https://news.google.com/rss/search?q=kinh+te+tai+chinh+viet+nam&hl=vi&gl=VN&ceid=VN:vi',
        'VN', TRUE),
    ('Google News - VN30 Index',
        'https://news.google.com/rss/search?q=VN30+index&hl=vi&gl=VN&ceid=VN:vi',
        'VN', TRUE),
    ('Google News - Vietnam Stock Market',
        'https://news.google.com/rss/search?q=vietnam+stock+market&hl=en&gl=VN&ceid=VN:en',
        'VN', TRUE)
ON CONFLICT (url) DO NOTHING;

-- ── International ─────────────────────────────────────────────────────────────
INSERT INTO rss_feeds (name, url, category, is_active) VALUES
    -- 17 items/run
    ('CNBC - Investing',
        'https://www.cnbc.com/id/15839069/device/rss/rss.html',                             'Global', TRUE),
    -- 10 items/run
    ('MarketWatch - Markets',
        'https://feeds.content.dowjones.io/public/rss/mw_realtimeheadlines',               'Global', TRUE),
    -- 42 items/run
    ('Yahoo Finance - News',
        'https://finance.yahoo.com/news/rssindex',                                          'Global', TRUE),
    -- 10 items/run
    ('Investing.com - Market News',
        'https://www.investing.com/rss/news_1.rss',                                         'Global', TRUE),
    -- 10 items/run
    ('Financial Times - Markets',
        'https://www.ft.com/?format=rss&taxonomy=sections&slug=markets',                   'Global', TRUE),
    -- 20 items/run
    ('Wall Street Journal - Markets',
        'https://feeds.a.dj.com/rss/RSSMarketsMain.xml',                                   'Global', TRUE)
ON CONFLICT (url) DO NOTHING;

-- NOT included (blocked/no valid RSS):
--   ❌ VnEconomy    — TLS fingerprint rejected (Cloudflare WAF)
--   ❌ Dân Trí      — RSS path removed, returns HTML page
--   ❌ Reuters feeds.reuters.com — domain no longer exists (deprecated 2020)
--   ❌ CafeF        — only has sitemap RSS, not article feed
