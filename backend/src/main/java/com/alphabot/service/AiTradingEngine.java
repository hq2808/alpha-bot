package com.alphabot.service;

import com.alphabot.dto.AiRecommendation;
import com.alphabot.dto.TradeOrderRequest;
import com.alphabot.entity.NewsArticle;
import com.alphabot.repository.NewsArticleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiTradingEngine {

    private final FinancialAssistant financialAssistant;
    private final NewsArticleRepository newsArticleRepository;
    private final PortfolioService portfolioService;
    private final MarketSessionService marketSessionService;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // Minimum AI Confidence required to execute a trade
    private static final double MIN_CONFIDENCE_THRESHOLD = 0.7;

    private static final String REDIS_KEY_TRADE_LOCK_PREFIX = "Trade:Lock:AutoTrade:";
    private static final String REDIS_KEY_AI_REC_LATEST = "AI:Recommendations:Latest";

    // Run automatically every weekday at 14:45 (Before ATC) to analyze and trade
    @Scheduled(cron = "0 45 14 * * MON-FRI", zone = "Asia/Ho_Chi_Minh")
    public void runAutoTradingJob() {
        if (!marketSessionService.isMarketOpen()) {
            log.info("AI Trading: Market is closed. Skipping auto-trade job.");
            return;
        }
        log.info("Starting AI Auto Trading Job for all users...");
        List<AiRecommendation> recommendations = analyzeMarket();

        // Fetch all AUTO portfolios to execute trades for each user
        List<com.alphabot.entity.Portfolio> allAutoPortfolios = portfolioService
                .getAllPortfoliosByType(com.alphabot.entity.PortfolioType.AUTO);

        for (com.alphabot.entity.Portfolio portfolio : allAutoPortfolios) {
            executeTradeDecisions(portfolio, recommendations, false);
        }
    }

    /**
     * Helper method to run Dry Run via Controller API
     */
    public List<TradeOrderRequest> executeDryRun() {
        log.info("Starting Dry-Run AI Trading Job...");
        List<AiRecommendation> recommendations = analyzeMarket();
        // Dry run doesn't need a real portfolio for execution logs, but we pass null or
        // a dummy if needed
        return executeTradeDecisions(null, recommendations, true);
    }

    private List<AiRecommendation> analyzeMarket() {
        // Find news crawled in the last 24 hours
        Instant yesterday = Instant.now().minus(java.time.Duration.ofDays(1));
        List<NewsArticle> todaysNews = newsArticleRepository.findByCrawledAtAfter(yesterday);

        if (todaysNews.isEmpty()) {
            log.info("AI Trading: No news found today.");
            return new ArrayList<>();
        }

        // Format news to string same as EOD report
        String newsContext = todaysNews.stream()
                .map(n -> String.format("- %s (Ticker: %s, Sentiment: %s): %s",
                        n.getPublishedAt(),
                        n.getMentionedTickers() != null ? n.getMentionedTickers() : "General",
                        n.getSentimentScore(),
                        n.getTitle()))
                .collect(Collectors.joining("\n"));

        log.info("AI Trading: Sending {} articles to LLM for signal analysis...", todaysNews.size());

        String jsonResponse = financialAssistant.analyzeTradingSignals(newsContext);

        try {
            // Clean markdown block if LLM accidentally outputs it
            jsonResponse = jsonResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            List<AiRecommendation> recommendations = objectMapper.readValue(jsonResponse,
                    new TypeReference<List<AiRecommendation>>() {
                    });
            log.info("AI Trading: Received {} recommendations from LLM.", recommendations.size());

            // Save latest recommendations to Redis Cache for UI to fetch instantly
            redisTemplate.opsForValue().set(REDIS_KEY_AI_REC_LATEST, recommendations, 24, TimeUnit.HOURS);

            return recommendations;
        } catch (JsonProcessingException e) {
            log.error("AI Trading: Failed to parse LLM JSON response: \n{}", jsonResponse, e);
            return new ArrayList<>();
        }
    }

    private List<TradeOrderRequest> executeTradeDecisions(com.alphabot.entity.Portfolio portfolio,
            List<AiRecommendation> recommendations, boolean isDryRun) {
        List<TradeOrderRequest> approvedOrders = new ArrayList<>();

        for (AiRecommendation rec : recommendations) {
            // -------------------------------------------------------------
            // LAYER 1: TRADE DECISION GUARDRAIL (RULE-BASED FILTER)
            // -------------------------------------------------------------

            // Rule 1: Ignore HOLD signals entirely for execution
            if ("HOLD".equalsIgnoreCase(rec.getAction())) {
                log.info("Guardrail [HOLD]: Ignoring HOLD signal for {}", rec.getTicker());
                continue;
            }

            // Rule 2: Minimum Confidence
            if (rec.getConfidence() < MIN_CONFIDENCE_THRESHOLD) {
                log.warn("Guardrail [REJECTED]: {} signal for {} has low confidence ({} < {}). Reason: {}",
                        rec.getAction(), rec.getTicker(), rec.getConfidence(), MIN_CONFIDENCE_THRESHOLD,
                        rec.getReason());
                continue;
            }

            // Rule 3: Missing ticker validation
            if (rec.getTicker() == null || rec.getTicker().isBlank()) {
                log.warn("Guardrail [REJECTED]: Ticker is null/blank");
                continue;
            }

            // Rule 4: Anti-Overtrade Lock via Redis (Max 1 trade per ticker per day)
            String today = LocalDate.now().toString();
            String lockKey = REDIS_KEY_TRADE_LOCK_PREFIX + rec.getTicker() + ":" + today;

            if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
                log.warn("Guardrail [REJECTED]: Anti-Overtrade mechanism triggered. AI already traded {} today.",
                        rec.getTicker());
                continue;
            }

            // -------------------------------------------------------------
            // APPROVED BY GUARDRAILS
            // -------------------------------------------------------------
            log.info("Guardrail [APPROVED]: {} {} (Confidence: {}). Reason: {}",
                    rec.getAction(), rec.getTicker(), rec.getConfidence(), rec.getReason());

            TradeOrderRequest order = new TradeOrderRequest();
            order.setAction(rec.getAction());
            order.setTicker(rec.getTicker());
            order.setReason(String.format("[AI Conf: %.2f] %s", rec.getConfidence(), rec.getReason()));

            approvedOrders.add(order);

            // Execute via Portfolio Service if NOT dry run
            // (Note: Position sizing & Max allocation rules are handled gracefully inside
            // PortfolioService.executeTrade)
            if (!isDryRun && portfolio != null) {
                try {
                    portfolioService.executeTrade(portfolio, order);
                    // Set Redis Lock for the rest of the day (24h) protecting from overtrading
                    redisTemplate.opsForValue().set(lockKey, "LOCKED", 24, TimeUnit.HOURS);
                } catch (Exception e) {
                    log.error("Failed to execute trade for {}: {}", rec.getTicker(), e.getMessage());
                }
            }
        }

        return approvedOrders;
    }
}
