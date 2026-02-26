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

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Scheduled service that crawls RSS feeds, analyzes sentiment via AI,
 * and pushes real-time updates via WebSocket.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NewsCrawlerService {

    private final NewsArticleRepository newsArticleRepository;
    private final SentimentAnalyzerService sentimentAnalyzer;
    private final SimpMessagingTemplate messagingTemplate;
    private final AlertService alertService;

    @Value("${alpha-bot.alert.sentiment-threshold:0.7}")
    private double alertThreshold;

    // --- News Feed Sources ---
    private static final List<Map<String, String>> FEEDS = List.of(
            Map.of("source", "Reuters", "url", "https://feeds.reuters.com/reuters/businessNews"),
            Map.of("source", "CNBC", "url", "https://www.cnbc.com/id/100003114/device/rss/rss.html"),
            Map.of("source", "Yahoo Finance", "url", "https://finance.yahoo.com/rss/topstories"),
            Map.of("source", "CoinDesk", "url", "https://www.coindesk.com/arc/outboundfeeds/rss/"),
            Map.of("source", "MarketWatch", "url", "https://feeds.content.dowjones.io/public/rss/mw_topstories"));

    /**
     * Main crawl job — runs every 5 minutes.
     */
    @Scheduled(fixedDelayString = "${alpha-bot.crawler.interval-ms:300000}")
    public void crawlAll() {
        log.info("[Crawler] Starting crawl cycle...");
        FEEDS.forEach(feed -> {
            try {
                crawlFeed(feed.get("source"), feed.get("url"));
            } catch (Exception e) {
                log.warn("[Crawler] Failed to crawl {}: {}", feed.get("source"), e.getMessage());
            }
        });
        log.info("[Crawler] Crawl cycle complete.");
    }

    private void crawlFeed(String source, String feedUrl) throws Exception {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed;
        try (XmlReader reader = new XmlReader(new URL(feedUrl))) {
            feed = input.build(reader);
        }

        int newItems = 0;
        for (SyndEntry entry : feed.getEntries()) {
            String url = entry.getLink();
            if (url == null || newsArticleRepository.existsByUrl(url))
                continue;

            // Build the article
            NewsArticle article = new NewsArticle();
            article.setUrl(url);
            article.setTitle(entry.getTitle());
            article.setDescription(entry.getDescription() != null ? entry.getDescription().getValue() : null);
            article.setSource(source);
            article.setPublishedAt(entry.getPublishedDate() != null
                    ? entry.getPublishedDate().toInstant()
                    : Instant.now());

            // AI Analysis (cached by title hash)
            var result = sentimentAnalyzer.analyze(article.getTitle(), article.getDescription());
            article.setSentimentScore(result.score());
            article.setMentionedTickers(result.tickers());
            article.setAiSummary(result.summary());

            newsArticleRepository.save(article);
            newItems++;

            // Push to WebSocket subscribers
            messagingTemplate.convertAndSend("/topic/news", article);

            // Check alert threshold -> send Telegram notification
            if (result.isBullish(alertThreshold)) {
                alertService.sendBullishAlert(article);
            }
        }
        if (newItems > 0) {
            log.info("[Crawler] {} | {} new articles saved.", source, newItems);
        }
    }
}
