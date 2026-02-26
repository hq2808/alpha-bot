package com.alphabot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "news_articles")
@Data
@NoArgsConstructor
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable = false)
    private String source;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "crawled_at", nullable = false)
    private Instant crawledAt = Instant.now();

    // AI-generated fields
    @Column(name = "sentiment_score")
    private Double sentimentScore;          // -1.0 (Bearish) to 1.0 (Bullish)

    @Column(name = "mentioned_tickers", length = 500)
    private String mentionedTickers;        // e.g., "AAPL,GOOGL,BTC"

    @Column(name = "ai_summary", length = 1000)
    private String aiSummary;

    @Column(name = "is_alert_sent")
    private boolean alertSent = false;
}
