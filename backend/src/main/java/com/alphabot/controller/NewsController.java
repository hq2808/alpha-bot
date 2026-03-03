package com.alphabot.controller;

import com.alphabot.dto.SentimentTrendResponse;
import com.alphabot.entity.NewsArticle;
import com.alphabot.repository.NewsArticleRepository;
import com.alphabot.repository.WatchlistRepository;
import com.alphabot.service.MarketInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:4200", "http://frontend:80" })
@io.swagger.v3.oas.annotations.tags.Tag(name = "News & Sentiments", description = "Endpoints for financial news, AI sentiment analysis, and market signals")
public class NewsController {

    private final NewsArticleRepository newsArticleRepository;
    private final WatchlistRepository watchlistRepository;
    private final MarketInsightService marketInsightService;
    private final com.alphabot.service.AlertService alertService;
    private final com.alphabot.service.ReportService reportService;

    /**
     * GET /api/news/latest — Returns the 20 most recent articles with AI analysis.
     * Optionally filtered by user's watchlist.
     */
    @GetMapping("/news/latest")
    @io.swagger.v3.oas.annotations.Operation(summary = "Get latest news", description = "Returns the 20 most recent articles with AI analysis. Can be filtered by user watchlist.")
    public ResponseEntity<List<NewsArticle>> getLatestNews(
            @RequestParam(defaultValue = "false") boolean filterByWatchlist) {
        if (filterByWatchlist) {
            List<String> userTickers = watchlistRepository.findAllTickersByUserId(null); // null until auth is added
            if (userTickers == null || userTickers.isEmpty()) {
                return ResponseEntity.ok(List.of()); // Semantic empty return for frontend
            }

            // Database-level filtering via Native Query
            String[] tickerArray = userTickers.toArray(new String[0]);
            List<NewsArticle> filtered = newsArticleRepository.findNewsByWatchlistTickers(tickerArray, 20);
            return ResponseEntity.ok(filtered);
        }
        return ResponseEntity.ok(newsArticleRepository.findTop20ByOrderByCrawledAtDesc());
    }

    /**
     * GET /api/news/bullish?threshold=0.5 — Returns bullish articles above a
     * threshold.
     */
    @GetMapping("/news/bullish")
    @io.swagger.v3.oas.annotations.Operation(summary = "Get bullish news", description = "Returns bullish articles above a specific sentiment threshold.")
    public ResponseEntity<List<NewsArticle>> getBullishNews(
            @RequestParam(defaultValue = "0.5") double threshold,
            @RequestParam(defaultValue = "false") boolean filterByWatchlist) {

        if (filterByWatchlist) {
            List<String> userTickers = watchlistRepository.findAllTickersByUserId(null);
            if (userTickers == null || userTickers.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }

            // DB-level filtering
            String[] tickerArray = userTickers.toArray(new String[0]);
            List<NewsArticle> filtered = newsArticleRepository.findBullishNewsByWatchlistTickers(threshold, tickerArray,
                    20);
            return ResponseEntity.ok(filtered);
        }

        List<NewsArticle> bullishArticles = newsArticleRepository
                .findBySentimentScoreGreaterThanAndAlertSentFalse(threshold);
        return ResponseEntity.ok(bullishArticles);
    }

    /**
     * GET /api/market/signals — Returns trending tickers with buy/sell signals
     * based on news.
     */
    @GetMapping("/market/signals")
    @io.swagger.v3.oas.annotations.Operation(summary = "Get market signals", description = "Returns trending tickers with buy/sell signals based on real-time news analysis.")
    public ResponseEntity<List<MarketInsightService.TickerSignal>> getMarketSignals() {
        return ResponseEntity.ok(marketInsightService.getMarketSignals());
    }

    /**
     * GET /api/news/search — Paginated + keyword search for the News Insight page.
     *
     * Params:
     * q = keyword to search (title / source / tickers / tags), default ""
     * page = 0-based page index, default 0
     * size = items per page, default 20
     * filterType = all | bull | bear (default all)
     *
     * Response: Spring Page object with content[] + totalPages + totalElements
     */
    @GetMapping("/news/search")
    @io.swagger.v3.oas.annotations.Operation(summary = "Search news articles", description = "Perform paginated keyword and ticker search on news articles with sentiment filtering.")
    public ResponseEntity<Page<NewsArticle>> searchNews(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) Integer hours,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "all") String filterType) {

        java.time.Instant since = hours != null
                ? java.time.Instant.now().minus(hours, java.time.temporal.ChronoUnit.HOURS)
                : java.time.Instant.EPOCH;
        String exactTicker = ticker != null ? ticker : "";

        Page<NewsArticle> result = newsArticleRepository.searchBullishArticles(
                filterType, q, exactTicker, since, PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/news/sentiment-trend — Daily avg sentiment + article count for last
     * 30 days.
     */
    @GetMapping("/news/sentiment-trend")
    @io.swagger.v3.oas.annotations.Operation(summary = "Get sentiment trend", description = "Returns daily average sentiment and article count for the last 30 days.")
    public ResponseEntity<List<SentimentTrendResponse>> getSentimentTrend() {
        List<Map<String, Object>> trendData = newsArticleRepository.sentimentTrendByDay();
        List<SentimentTrendResponse> result = trendData.stream()
                .map(m -> SentimentTrendResponse.builder()
                        .date(LocalDate.parse((String) m.get("date")))
                        .avgSentiment(((Number) m.get("avg_sentiment")).doubleValue())
                        .articleCount(((Number) m.get("article_count")).longValue())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/health — Simple health check endpoint.
     */
    @GetMapping("/health")
    @io.swagger.v3.oas.annotations.Operation(summary = "API Health Check", description = "Simple endpoint to verify if the News API is operational.")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK", "version", "1.1.0"));
    }

    /**
     * GET /api/test-alert — Temporary endpoint to test Telegram notifications.
     */
    @GetMapping("/test-alert")
    @io.swagger.v3.oas.annotations.Operation(summary = "Trigger test alert", description = "Sends a dummy bullish alert to Telegram for testing purposes.")
    public ResponseEntity<String> testAlert() {
        NewsArticle dummyArticle = new NewsArticle();
        dummyArticle.setTitle("Test Title from AlphaBot!");
        dummyArticle.setSource("AlphaBot Tester");
        dummyArticle.setUrl("https://example.com/" + java.util.UUID.randomUUID().toString());
        dummyArticle.setSentimentScore(0.99);
        dummyArticle.setAiSummary("This is a test summary to verify Telegram bot connection.");
        dummyArticle.setMentionedTickers("FPT, VNM");
        // Không save vào DB — chỉ gửi Telegram test, không xuất hiện ở FE hay báo cáo
        alertService.sendBullishAlert(dummyArticle);
        return ResponseEntity.ok("Alert triggered for dummy article!");
    }

    /**
     * GET /api/test-eod-report — Manual trigger for the End of Day Telegram summary
     * report.
     */
    @GetMapping("/test-eod-report")
    @io.swagger.v3.oas.annotations.Operation(summary = "Trigger test EOD report", description = "Manually generates and dispatches the End-Of-Day Telegram summary report.")
    public ResponseEntity<String> testEodReport() {
        reportService.generateAndSendEodReport();
        return ResponseEntity.ok("EOD Telegram report generated and dispatched.");
    }
}
