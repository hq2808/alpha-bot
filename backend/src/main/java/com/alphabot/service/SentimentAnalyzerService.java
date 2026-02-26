package com.alphabot.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * AI service for sentiment analysis and ticker extraction.
 * Uses LangChain4j to communicate with Groq (cloud) or Ollama (local).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SentimentAnalyzerService {

    private final ChatLanguageModel chatLanguageModel;

    @Value("${alpha-bot.ai.provider:groq}")
    private String aiProvider;

    private static final String SENTIMENT_PROMPT = """
            You are a financial news analyst. Analyze the following news article title and description.

            Your task:
            1. Rate the overall sentiment for the stock market on a scale from -1.0 to 1.0
               (-1.0 = Very Bearish, 0.0 = Neutral, 1.0 = Very Bullish)
            2. Extract any stock tickers mentioned (e.g., AAPL, BTC, GOOGL). If none mentioned, return empty.
            3. Provide a 1-sentence summary.

            Respond ONLY in this exact JSON format:
            {"score": 0.5, "tickers": "AAPL,GOOGL", "summary": "Your summary here."}

            Article Title: %s
            Article Description: %s
            """;

    /**
     * Analyze sentiment of a news article.
     * Results are cached by a hash of the title to avoid redundant AI calls.
     */
    @Cacheable(value = "sentiment", key = "#title.hashCode()")
    public SentimentResult analyze(String title, String description) {
        try {
            String prompt = SENTIMENT_PROMPT.formatted(title,
                    description != null ? description.substring(0, Math.min(description.length(), 500)) : "");

            log.debug("[AI:{}] Analyzing: {}", aiProvider, title.substring(0, Math.min(title.length(), 60)));
            String response = chatLanguageModel.generate(prompt);

            return parseSentimentResponse(response);
        } catch (Exception e) {
            log.warn("[AI] Failed to analyze sentiment for: {}. Error: {}", title, e.getMessage());
            return SentimentResult.neutral();
        }
    }

    private SentimentResult parseSentimentResponse(String json) {
        try {
            // Simple regex-based parse to avoid Jackson dependency for this light use case
            double score = Double.parseDouble(json.replaceAll(".*\"score\":\\s*([\\-0-9.]+).*", "$1"));
            String tickers = json.replaceAll(".*\"tickers\":\\s*\"([^\"]*)\".*", "$1");
            String summary = json.replaceAll(".*\"summary\":\\s*\"([^\"]*)\".*", "$1");
            return new SentimentResult(score, tickers, summary);
        } catch (Exception e) {
            log.warn("[AI] Failed to parse AI response: {}", json);
            return SentimentResult.neutral();
        }
    }

    public record SentimentResult(double score, String tickers, String summary) {
        public static SentimentResult neutral() {
            return new SentimentResult(0.0, "", "");
        }

        public boolean isBullish(double threshold) {
            return score >= threshold;
        }
    }
}
