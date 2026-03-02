package com.alphabot.service;

import com.alphabot.entity.NewsArticle;
import com.alphabot.entity.RssFeed;
import com.alphabot.repository.NewsArticleRepository;
import com.alphabot.repository.RssFeedRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Scheduled crawler for Vietnamese stock market RSS feeds.
 * Uses keyword-based sentiment analysis — no API keys required.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NewsCrawlerService {

    private final NewsArticleRepository newsArticleRepository;
    private final SentimentAnalyzerService sentimentAnalyzer;
    private final SimpMessagingTemplate messagingTemplate;
    private final AlertService alertService;
    private final RssFeedRepository rssFeedRepository;

    @Value("${alpha-bot.alert.sentiment-threshold:0.6}")
    private double alertThreshold;

    // Browser User-Agents to avoid being blocked by Vietnamese news sites
    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:122.0) Gecko/20100101 Firefox/122.0",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

    @Scheduled(fixedDelayString = "${alpha-bot.crawler.interval-ms:300000}")
    public void crawlAll() {
        List<RssFeed> activeFeeds = rssFeedRepository.findByIsActiveTrue();
        if (activeFeeds.isEmpty()) {
            log.warn("[Crawler] No active RSS feeds found in DB. Skipping crawl cycle.");
            return;
        }
        log.info("[Crawler] Starting crawl cycle with {} active feeds...", activeFeeds.size());
        activeFeeds.forEach(feed -> {
            try {
                crawlFeed(feed.getName(), feed.getUrl());
            } catch (Exception e) {
                log.warn("[Crawler] Failed to crawl {}: {} — {}", feed.getName(), e.getMessage(),
                        e.getCause() != null ? e.getCause().getMessage() : "no cause");
            }
        });
        log.info("[Crawler] Crawl cycle complete.");
    }

    private void crawlFeed(String source, String feedUrl) throws Exception {
        // Fetch with random browser User-Agent so Vietnamese sites don't block us
        HttpURLConnection conn = (HttpURLConnection) URI.create(feedUrl).toURL().openConnection();
        String randomUserAgent = USER_AGENTS.get(new java.util.Random().nextInt(USER_AGENTS.size()));
        conn.setRequestProperty("User-Agent", randomUserAgent);
        conn.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);

        // Read and sanitize: Vietnamese sites embed invalid HTML in RSS descriptions
        // e.g. VnExpress uses </br> (not valid XML) which crashes Rome parser
        byte[] rawBytes = conn.getInputStream().readAllBytes();
        String rawXml = new String(rawBytes, StandardCharsets.UTF_8);

        rawXml = com.alphabot.utils.TextProcessingUtils.sanitizeHtmlForRss(rawXml);
        rawXml = com.alphabot.utils.TextProcessingUtils.fixUnescapedAmpersands(rawXml);

        SyndFeed feed;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(rawXml.getBytes(StandardCharsets.UTF_8));
                XmlReader reader = new XmlReader(bais)) {
            feed = new SyndFeedInput().build(reader);
        }

        int newItems = 0;
        for (SyndEntry entry : feed.getEntries()) {
            String url = entry.getLink();
            if (url == null || newsArticleRepository.existsByUrl(url))
                continue;

            NewsArticle article = new NewsArticle();
            article.setUrl(url);
            article.setTitle(entry.getTitle());
            article.setDescription(entry.getDescription() != null
                    ? entry.getDescription().getValue()
                    : null);
            article.setSource(source);
            article.setPublishedAt(entry.getPublishedDate() != null
                    ? entry.getPublishedDate().toInstant()
                    : Instant.now());

            // Keyword-based sentiment (offline, no API key)
            com.alphabot.service.SentimentAnalyzerService.SentimentResult result = sentimentAnalyzer
                    .analyze(article.getTitle(), article.getDescription());
            article.setSentimentScore(result.score());
            article.setMentionedTickers(result.tickers());
            article.setAiSummary(result.summary());
            article.setTags(result.tags());

            newsArticleRepository.save(article);
            newItems++;

            // Push real-time to WebSocket subscribers
            messagingTemplate.convertAndSend("/topic/news", article);

            // Telegram alert: only if bullish AND article is financially relevant.
            // Guard against false positives: sports/entertainment articles can score
            // high due to words like "kỷ lục" (record), "bứt phá" (breakout), etc.
            if (result.isBullish(alertThreshold) && isFinanciallyRelevant(result)) {
                alertService.sendBullishAlert(article);
            }
        }

        if (newItems > 0) {
            log.info("[Crawler] {} | {} new articles saved.", source, newItems);
        }
    }

    /**
     * Guards against false-positive Telegram alerts from sports/entertainment
     * articles.
     *
     * An article is financially relevant if it has:
     * (a) at least one recognized stock ticker (e.g. FPT, VNM, AAPL), OR
     * (b) a specific financial domain tag (Vĩ Mô, Ngân Hàng, Bất Động Sản, Cổ Tức,
     * Chính Sách)
     *
     * Articles tagged only [Thị Trường] (the fallback default) are NOT considered
     * financially relevant on their own, since any article can get this default
     * tag.
     *
     * Example of false positive WITHOUT this guard:
     * "Yamal phá kỷ lục của Messi" → kỷ lục = VERY_BULLISH → score 0.76 → alert
     * sent ❌
     * With guard: no ticker, tag = [Thị Trường] → isFinanciallyRelevant = false ✅
     */
    private boolean isFinanciallyRelevant(SentimentAnalyzerService.SentimentResult result) {
        boolean hasTicker = result.tickers() != null && !result.tickers().isBlank();
        boolean hasFinancialTag = result.tags() != null && (result.tags().contains("[Vĩ Mô]") ||
                result.tags().contains("[Ngân Hàng]") ||
                result.tags().contains("[Bất Động Sản]") ||
                result.tags().contains("[Cổ Tức]") ||
                result.tags().contains("[Chính Sách]") ||
                result.tags().contains("[Công Bố HOSE]"));
        return hasTicker || hasFinancialTag;
    }
}
