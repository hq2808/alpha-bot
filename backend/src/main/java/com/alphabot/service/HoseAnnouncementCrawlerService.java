package com.alphabot.service;

import com.alphabot.entity.NewsArticle;
import com.alphabot.entity.SentimentCategory;
import com.alphabot.repository.NewsArticleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
// import org.springframework.scheduling.annotation.Scheduled; // re-enable when HOSE API endpoint is confirmed
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Polls HOSE (Ho Chi Minh Stock Exchange) official disclosure page every 2
 * minutes.
 *
 * Why this is faster than RSS:
 * - Companies are legally required to submit disclosures immediately.
 * - News sites may take 30–60 min to cover the same announcement.
 * - This scrapes the HOSE JSON API used internally by their web UI.
 *
 * Endpoint: GET https://www.hsx.vn/Modules/Listed/Web/Disclosure
 * (returns HTML table; AJAX endpoint returns JSON)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HoseAnnouncementCrawlerService {

    private final NewsArticleRepository newsArticleRepository;
    private final SentimentAnalyzerService sentimentAnalyzer;
    private final AlertService alertService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Value("${alpha-bot.alert.sentiment-threshold:0.6}")
    private double alertThreshold;

    // HOSE internal JSON API — same endpoint the web UI uses via AJAX
    private static final String HOSE_API = "https://www.hsx.vn/Modules/Listed/Web/Disclosure?pageFieldName1=Code&pageFieldValue1="
            +
            "&pageFieldName2=DateFrom&pageFieldValue2=&pageFieldName3=DateTo&pageFieldValue3=" +
            "&pageCriteriaAdv=&_search=false&nd=%d&rows=20&page=1&sidx=date&sord=desc";

    private static final String SOURCE_HOSE = "HOSE Official";

    /**
     * TEMPORARILY DISABLED: HOSE endpoint returns HTML, not JSON.
     * The internal AJAX API requires session cookies / different endpoint.
     * TODO: investigate correct HOSE JSON API endpoint.
     */
    // @Scheduled(fixedDelayString = "${alpha-bot.hose.interval-ms:120000}")
    public void crawlHoseDisclosures() {
        log.debug("[HOSE] Polling official disclosures...");
        try {
            List<NewsArticle> newArticles = fetchHoseDisclosures();
            log.info("[HOSE] Fetched {} new disclosures", newArticles.size());
        } catch (Exception e) {
            log.warn("[HOSE] Crawl failed: {}", e.getMessage());
        }
    }

    private List<NewsArticle> fetchHoseDisclosures() throws Exception {
        long timestamp = System.currentTimeMillis();
        String url = String.format(HOSE_API, timestamp);

        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/121.0.0.0 Safari/537.36");
        conn.setRequestProperty("Accept", "application/json, text/javascript, */*");
        conn.setRequestProperty("Referer", "https://www.hsx.vn/Modules/Listed/Web/Disclosure");
        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        int status = conn.getResponseCode();
        if (status != 200) {
            log.warn("[HOSE] HTTP {} from disclosure endpoint", status);
            return List.of();
        }

        String body = new String(conn.getInputStream().readAllBytes());
        return parseHoseResponse(body);
    }

    private List<NewsArticle> parseHoseResponse(String body) {
        List<NewsArticle> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode rows = root.path("rows");
            if (!rows.isArray()) {
                log.debug("[HOSE] No rows in response, might be HTML page — skipping");
                return results;
            }

            for (JsonNode row : rows) {
                String ticker = row.path("cell").get(0).asText("").trim();
                String title = row.path("cell").get(1).asText("").trim();
                String fileUrl = row.path("cell").get(3).asText("").trim();

                if (title.isBlank())
                    continue;

                // Skip if already processed
                if (newsArticleRepository.existsByTitleAndSource(title, SOURCE_HOSE))
                    continue;

                SentimentAnalyzerService.SentimentResult sentiment = sentimentAnalyzer.analyze(title, ticker);

                NewsArticle article = new NewsArticle();
                article.setTitle("[" + ticker + "] " + title);
                article.setSource(SOURCE_HOSE);
                article.setUrl(fileUrl.isBlank() ? "https://www.hsx.vn/Modules/Listed/Web/Disclosure" : fileUrl);
                article.setMentionedTickers(ticker);
                article.setSentimentScore(sentiment.score());
                article.setAiSummary(sentiment.summary());
                article.setTags("[Công Bố HOSE]");
                article.setCrawledAt(Instant.now());

                NewsArticle saved = newsArticleRepository.save(article);

                // Push to WebSocket
                messagingTemplate.convertAndSend("/topic/news", saved);

                // Alert if bullish and above threshold
                if (sentiment.score() >= alertThreshold &&
                        sentiment.category() == SentimentCategory.BULLISH) {
                    alertService.sendBullishAlert(saved);
                }
                results.add(saved);
            }
        } catch (Exception e) {
            log.warn("[HOSE] Parse error: {}", e.getMessage());
        }
        return results;
    }
}
