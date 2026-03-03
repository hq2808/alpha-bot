package com.alphabot.repository;

import com.alphabot.entity.NewsArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

        boolean existsByUrl(String url);

        boolean existsByTitleAndSource(String title, String source);

        List<NewsArticle> findTop20ByOrderByCrawledAtDesc();

        // ── Paginated keyword search — used by News Insight page ─────────────────
        @Query("""
                        SELECT n FROM NewsArticle n
                        WHERE (
                            (:filterType = 'bull' AND n.sentimentScore >= 0.2)
                            OR (:filterType = 'bear' AND n.sentimentScore <= -0.2)
                            OR (:filterType = 'all')
                        )
                        AND (
                            :keyword IS NULL OR :keyword = ''
                            OR LOWER(n.title)             LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(n.source)            LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(n.mentionedTickers)  LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(n.tags)              LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )
                        AND (
                            :exactTicker = ''
                            OR LOWER(n.mentionedTickers) LIKE LOWER(CONCAT('%', :exactTicker, '%'))
                        )
                        AND n.crawledAt >= :since
                        ORDER BY COALESCE(n.publishedAt, n.crawledAt) DESC
                        """)
        Page<NewsArticle> searchBullishArticles(
                        @Param("filterType") String filterType,
                        @Param("keyword") String keyword,
                        @Param("exactTicker") String exactTicker,
                        @Param("since") Instant since,
                        Pageable pageable);

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

        // ── Sentiment trend: daily avg score + count for last 30 days ─────────────
        @Query(value = """
                        SELECT
                            TO_CHAR(COALESCE(published_at, crawled_at), 'YYYY-MM-DD') AS date,
                            ROUND(CAST(AVG(sentiment_score) AS numeric), 4)            AS avg_sentiment,
                            COUNT(*)                                                    AS article_count
                        FROM news_articles
                        WHERE COALESCE(published_at, crawled_at) >= NOW() - INTERVAL '30 days'
                        GROUP BY TO_CHAR(COALESCE(published_at, crawled_at), 'YYYY-MM-DD')
                        ORDER BY date ASC
                        """, nativeQuery = true)
        List<Map<String, Object>> sentimentTrendByDay();
}
