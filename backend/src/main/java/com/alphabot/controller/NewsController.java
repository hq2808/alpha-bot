package com.alphabot.controller;

import com.alphabot.entity.NewsArticle;
import com.alphabot.repository.NewsArticleRepository;
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

    /**
     * GET /api/news/latest — Returns the 20 most recent articles with AI analysis.
     */
    @GetMapping("/news/latest")
    public ResponseEntity<List<NewsArticle>> getLatestNews() {
        return ResponseEntity.ok(newsArticleRepository.findTop20ByOrderByCrawledAtDesc());
    }

    /**
     * GET /api/news/bullish?threshold=0.5 — Returns bullish articles above a
     * threshold.
     */
    @GetMapping("/news/bullish")
    public ResponseEntity<List<NewsArticle>> getBullishNews(
            @RequestParam(defaultValue = "0.5") double threshold) {
        return ResponseEntity.ok(
                newsArticleRepository.findBySentimentScoreGreaterThanAndAlertSentFalse(threshold));
    }

    /**
     * GET /api/health — Simple health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK", "version", "1.0.0"));
    }
}
