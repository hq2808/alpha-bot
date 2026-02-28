package com.alphabot.service;

import com.alphabot.entity.NewsArticle;
import com.alphabot.repository.NewsArticleRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsAgentTools {

    private final NewsArticleRepository newsArticleRepository;

    @Tool("Get the recent top news articles and their sentiment summary across the market.")
    public String getRecentMarketNews() {
        log.info("[AI Tool] Fetching recent market news.");
        List<NewsArticle> articles = newsArticleRepository.findTop20ByOrderByCrawledAtDesc();
        if (articles.isEmpty()) {
            return "No recent news available in the database.";
        }
        return articles.stream()
                .map(a -> String.format("- Source: %s\n  Title: %s\n  Summary: %s\n  Sentiment: %.2f (Tickers: %s)",
                        a.getSource(), a.getTitle(), a.getAiSummary(), a.getSentimentScore(), a.getMentionedTickers()))
                .collect(Collectors.joining("\n\n"));
    }
}
