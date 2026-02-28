package com.alphabot.controller;

import com.alphabot.entity.NewsArticle;
import com.alphabot.repository.NewsArticleRepository;
import com.alphabot.repository.WatchlistRepository;
import com.alphabot.service.MarketInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:4200", "http://frontend:80" })
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
    public ResponseEntity<List<MarketInsightService.TickerSignal>> getMarketSignals() {
        return ResponseEntity.ok(marketInsightService.getMarketSignals());
    }

    /**
     * GET /api/health — Simple health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK", "version", "1.1.0"));
    }

    /**
     * GET /api/test-alert — Temporary endpoint to test Telegram notifications.
     */
    @GetMapping("/test-alert")
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
    public ResponseEntity<String> testEodReport() {
        reportService.generateAndSendEodReport();
        return ResponseEntity.ok("EOD Telegram report generated and dispatched.");
    }
}
