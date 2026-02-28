package com.alphabot.service;

import com.alphabot.entity.NewsArticle;
import com.alphabot.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketInsightService {

    private final NewsArticleRepository newsArticleRepository;

    public record TickerSignal(
            String ticker,
            double averageSentiment,
            int mentionCount,
            String signal,
            String lastNewsTitle) {
    }

    /**
     * Aggregates recent news (last 24h) and generates signals per ticker.
     */
    public List<TickerSignal> getMarketSignals() {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        List<NewsArticle> recentNews = newsArticleRepository.findByCrawledAtAfter(since);

        Map<String, List<NewsArticle>> tickerMap = new HashMap<>();

        for (NewsArticle article : recentNews) {
            if (article.getMentionedTickers() != null && !article.getMentionedTickers().isBlank()) {
                String[] tickers = article.getMentionedTickers().split(",");
                for (String ticker : tickers) {
                    tickerMap.computeIfAbsent(ticker.trim(), k -> new ArrayList<>()).add(article);
                }
            }
        }

        return tickerMap.entrySet().stream()
                .map(entry -> {
                    String ticker = entry.getKey();
                    List<NewsArticle> articles = entry.getValue();
                    double avgSentiment = articles.stream()
                            .mapToDouble(NewsArticle::getSentimentScore)
                            .average()
                            .orElse(0.0);

                    String signal = determineSignal(avgSentiment, articles.size());
                    String lastTitle = articles.get(0).getTitle();

                    return new TickerSignal(ticker, avgSentiment, articles.size(), signal, lastTitle);
                })
                .sorted(Comparator.comparing(TickerSignal::mentionCount).reversed())
                .limit(10) // Top 10 trending tickers
                .collect(Collectors.toList());
    }

    private String determineSignal(double avgScore, int count) {
        if (count >= 3) {
            if (avgScore >= 0.7)
                return "STRONG BUY";
            if (avgScore <= -0.7)
                return "STRONG SELL";
        }

        if (avgScore >= 0.4)
            return "BUY";
        if (avgScore <= -0.4)
            return "SELL";

        return "NEUTRAL";
    }
}
