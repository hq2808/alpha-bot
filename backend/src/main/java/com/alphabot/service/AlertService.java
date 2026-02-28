package com.alphabot.service;

import com.alphabot.entity.NewsArticle;
import com.alphabot.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.alphabot.repository.WatchlistRepository;
import com.alphabot.entity.Watchlist;

import java.util.List;

/**
 * Telegram alert service. Sends a message when a bullish article is detected.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private final NewsArticleRepository newsArticleRepository;
    private final RestTemplate restTemplate;
    private final WatchlistRepository watchlistRepository;

    @Value("${alpha-bot.telegram.bot-token:}")
    private String botToken;

    @Value("${alpha-bot.telegram.chat-id:}")
    private String chatId;

    public void sendBullishAlert(NewsArticle article) {
        if (botToken.isBlank() || chatId.isBlank())
            return;
        if (article.isAlertSent())
            return;

        // Watchlist filtering
        List<Watchlist> watchlist = watchlistRepository.findAll();
        if (!watchlist.isEmpty()) {
            boolean isWatched = false;
            String tickers = article.getMentionedTickers();
            if (tickers != null && !tickers.isBlank()) {
                for (Watchlist w : watchlist) {
                    if (tickers.contains(w.getTicker())) {
                        isWatched = true;
                        break;
                    }
                }
            }
            if (!isWatched) {
                return; // Ignore article, not in watchlist
            }
        }

        String message = String.format(
                "📈 *BULLISH SIGNAL DETECTED*\n\n" +
                        "*Source:* %s\n" +
                        "*Title:* %s\n" +
                        "*Tickers:* %s\n" +
                        "*Sentiment:* %.2f/1.0\n" +
                        "*Summary:* %s\n\n" +
                        "[Read more](%s)",
                article.getSource(),
                escapeMarkdown(article.getTitle()),
                article.getMentionedTickers().isBlank() ? "N/A" : article.getMentionedTickers(),
                article.getSentimentScore(),
                escapeMarkdown(article.getAiSummary()),
                article.getUrl());

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            var payload = new java.util.HashMap<String, String>();
            payload.put("chat_id", chatId);
            payload.put("text", message);
            payload.put("parse_mode", "Markdown");
            restTemplate.postForObject(url, payload, String.class);

            article.setAlertSent(true);
            newsArticleRepository.save(article);
            log.info("[Alert] Telegram sent for: {}", article.getTitle());
        } catch (Exception e) {
            log.warn("[Alert] Failed to send Telegram alert: {}", e.getMessage());
        }
    }

    /**
     * Send End of Day Summary Report using MarkdownV2 format.
     */
    public void sendEodReport(String rawReport) {
        if (botToken.isBlank() || chatId.isBlank())
            return;

        // Escape strict MarkdownV2 chars for Telegram API
        String escapedText = escapeForMarkdownV2(rawReport);
        String finalMessage = "📊 *Tổng Hợp Thị Trường Cuối Ngày*\n\n" + escapedText;

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            var payload = new java.util.HashMap<String, String>();
            payload.put("chat_id", chatId);
            payload.put("text", finalMessage);
            payload.put("parse_mode", "MarkdownV2");
            restTemplate.postForObject(url, payload, String.class);

            log.info("[Alert] EOD Report sent successfully to Telegram.");
        } catch (Exception e) {
            log.warn("[Alert] Failed to send EOD Telegram report: {}", e.getMessage());
        }
    }

    /**
     * Safely escapes characters required by Telegram's MarkdownV2
     * while preserving asterisks (*) for bold formatting.
     * LLMs often produce **bold** which we map to *bold* first.
     */
    private String escapeForMarkdownV2(String text) {
        if (text == null)
            return "";

        // 1. Convert LLM **bold** to Telegram *bold*
        String processed = text.replaceAll("\\*\\*", "*");

        // 2. Escape all mandatory MarkdownV2 characters EXCEPT * (asterisk)
        // Mandatory escaped chars: _ * [ ] ( ) ~ ` > # + - = | { } . !
        // We do not escape * so that formatting triggers.
        return processed
                .replace("_", "\\_")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-") // In Telegram lists prefer emojis bc escaping hyphen is hard
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }

    private String escapeMarkdown(String text) {
        if (text == null)
            return "";
        return text.replaceAll("[_*\\[\\]()~`>#+=|{}.!-]", "\\\\$0");
    }
}
