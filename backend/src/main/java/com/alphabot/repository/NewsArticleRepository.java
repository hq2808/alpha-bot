package com.alphabot.repository;

import com.alphabot.entity.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    boolean existsByUrl(String url);

    List<NewsArticle> findTop20ByOrderByCrawledAtDesc();

    // Find articles above a sentiment threshold that haven't triggered an alert
    List<NewsArticle> findBySentimentScoreGreaterThanAndAlertSentFalse(Double threshold);

    // Get recent articles mentioning specific tickers
    @Query("SELECT n FROM NewsArticle n WHERE n.mentionedTickers LIKE %:ticker% " +
            "AND n.crawledAt > :since ORDER BY n.crawledAt DESC")
    List<NewsArticle> findRecentByTicker(String ticker, Instant since);
}
