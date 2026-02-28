package com.alphabot.repository;

import com.alphabot.entity.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    boolean existsByUrl(String url);

    List<NewsArticle> findTop20ByOrderByCrawledAtDesc();

    // Find articles above a sentiment threshold that haven't triggered an alert
    List<NewsArticle> findBySentimentScoreGreaterThanAndAlertSentFalse(Double threshold);

    // Get all articles since a specific time
    List<NewsArticle> findByCrawledAtAfter(Instant since);

    List<NewsArticle> findTop50ByOrderByCrawledAtDesc();

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT n.* FROM news_articles n
            WHERE n.mentioned_tickers IS NOT NULL
            AND EXISTS (
                SELECT 1 FROM unnest(CAST(:tickers AS text[])) t
                WHERE n.mentioned_tickers LIKE CONCAT('%', t, '%')
            )
            ORDER BY n.crawled_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<NewsArticle> findNewsByWatchlistTickers(
            @org.springframework.data.repository.query.Param("tickers") String[] tickers,
            @org.springframework.data.repository.query.Param("limit") int limit);

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT n.* FROM news_articles n
            WHERE n.sentiment_score >= :threshold
            AND n.is_alert_sent = false
            AND n.mentioned_tickers IS NOT NULL
            AND EXISTS (
                SELECT 1 FROM unnest(CAST(:tickers AS text[])) t
                WHERE n.mentioned_tickers LIKE CONCAT('%', t, '%')
            )
            ORDER BY n.crawled_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<NewsArticle> findBullishNewsByWatchlistTickers(
            @org.springframework.data.repository.query.Param("threshold") double threshold,
            @org.springframework.data.repository.query.Param("tickers") String[] tickers,
            @org.springframework.data.repository.query.Param("limit") int limit);
}
