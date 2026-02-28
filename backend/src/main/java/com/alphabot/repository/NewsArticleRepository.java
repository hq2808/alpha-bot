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
}
