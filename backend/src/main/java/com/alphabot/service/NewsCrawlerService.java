package com.alphabot.service;

import com.alphabot.entity.NewsArticle;
import com.alphabot.repository.NewsArticleRepository;
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
import java.util.Map;

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

    @Value("${alpha-bot.alert.sentiment-threshold:0.6}")
    private double alertThreshold;

    // ── Verified Working RSS Feeds (tested from Docker) ──────────────────
    private static final List<Map<String, String>> FEEDS = List.of(
            // ✅ Vietnamese — confirmed working
            Map.of("source", "VnExpress", "url", "https://vnexpress.net/rss/kinh-doanh.rss"),
            Map.of("source", "VnEconomy", "url", "https://vneconomy.vn/chung-khoan.rss"),
            // ✅ International — reliable fallback with financial coverage
            Map.of("source", "Reuters", "url", "https://feeds.reuters.com/reuters/businessNews"),
            Map.of("source", "CNBC", "url", "https://www.cnbc.com/id/100003114/device/rss/rss.html"));

    // Browser User-Agent to avoid being blocked by Vietnamese news sites
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/121.0.0.0 Safari/537.36";

    @Scheduled(fixedDelayString = "${alpha-bot.crawler.interval-ms:300000}")
    public void crawlAll() {
        log.info("[Crawler] Starting crawl cycle...");
        FEEDS.forEach(feed -> {
            try {
                crawlFeed(feed.get("source"), feed.get("url"));
            } catch (Exception e) {
                log.warn("[Crawler] Failed to crawl {}: {} — {}", feed.get("source"), e.getMessage(),
                        e.getCause() != null ? e.getCause().getMessage() : "no cause");
            }
        });
        log.info("[Crawler] Crawl cycle complete.");
    }

    private void crawlFeed(String source, String feedUrl) throws Exception {
        // Fetch with browser User-Agent so Vietnamese sites don't block us
        HttpURLConnection conn = (HttpURLConnection) URI.create(feedUrl).toURL().openConnection();
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);

        // Read and sanitize: Vietnamese sites embed invalid HTML in RSS descriptions
        // e.g. VnExpress uses </br> (not valid XML) which crashes Rome parser
        byte[] rawBytes = conn.getInputStream().readAllBytes();
        String rawXml = new String(rawBytes, StandardCharsets.UTF_8)
                .replace("</br>", "") // Invalid closing br tag
                .replace("<br>", " "); // Unclosed br tag — convert to space

        // Fix unescaped & in URLs inside CDATA (common in Vietnamese RSS)
        rawXml = rawXml.replaceAll("&(?!amp;|lt;|gt;|quot;|apos;|#)", "&amp;");

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
            var result = sentimentAnalyzer.analyze(article.getTitle(), article.getDescription());
            article.setSentimentScore(result.score());
            article.setMentionedTickers(result.tickers());
            article.setAiSummary(result.summary());

            newsArticleRepository.save(article);
            newItems++;

            // Push real-time to WebSocket subscribers
            messagingTemplate.convertAndSend("/topic/news", article);

            // Optional Telegram alert (only if bot-token is configured)
            if (result.isBullish(alertThreshold)) {
                alertService.sendBullishAlert(article);
            }
        }

        if (newItems > 0) {
            log.info("[Crawler] {} | {} new articles saved.", source, newItems);
        }
    }
}
