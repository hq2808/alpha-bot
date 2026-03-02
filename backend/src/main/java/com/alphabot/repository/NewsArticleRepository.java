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
    @Query(value = """
            SELECT * FROM news_articles
            WHERE sentiment_score >= :threshold
            AND (
                :keyword IS NULL OR :keyword = ''
                OR LOWER(title)             LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(source)            LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(mentioned_tickers) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(tags)              LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            ORDER BY COALESCE(published_at, crawled_at) DESC
            """, countQuery = """
            SELECT COUNT(*) FROM news_articles
            WHERE sentiment_score >= :threshold
            AND (
                :keyword IS NULL OR :keyword = ''
                OR LOWER(title)             LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(source)            LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(mentioned_tickers) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(tags)              LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """, nativeQuery = true)
    Page<NewsArticle> searchBullishArticles(
            @Param("threshold") double threshold,
            @Param("keyword") String keyword,
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
