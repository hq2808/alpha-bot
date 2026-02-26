package com.alphabot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;

/**
 * Rule-based financial sentiment analyzer using keyword dictionaries.
 * No external API required — works 100% offline.
 *
 * Scores:
 * +1.0 = very bullish (e.g. "record high", "beat expectations")
 * +0.5 = bullish (e.g. "growth", "profit")
 * 0.0 = neutral
 * -0.5 = bearish (e.g. "decline", "loss")
 * -1.0 = very bearish (e.g. "crash", "bankruptcy")
 */
@Service
@Slf4j
public class SentimentAnalyzerService {

    // ── Very Bullish keywords (score +1.0) ────────────────────────────
    private static final List<String> VERY_BULLISH = List.of(
            "record high", "all-time high", "beat expectations", "blowout earnings",
            "massive rally", "skyrocket", "surge", "soar", "moon", "breakout",
            "historic gain", "bull run", "explosive growth",
            // Vietnamese
            "kỷ lục", "bứt phá", "tăng mạnh nhất", "đỉnh lịch sử",
            "vượt kỳ vọng", "tăng trần", "tăng mạnh");

    // ── Bullish keywords (score +0.5) ─────────────────────────────────
    private static final List<String> BULLISH = List.of(
            "growth", "profit", "gain", "rise", "rally", "upturn", "rebound",
            "recovery", "upgrade", "beat", "outperform", "buy", "positive",
            "strong", "bullish", "expand", "increase", "boost", "opportunity",
            "optimism", "confidence", "investment", "revenue", "earnings beat",
            "partnership", "deal", "acquisition", "innovation", "exceeded",
            // Vietnamese
            "tăng", "lợi nhuận", "tăng trưởng", "khởi sắc", "phục hồi",
            "tích cực", "mua vào", "dòng tiền vào", "vượt kế hoạch",
            "cổ tức cao", "ngoại khối mua ròng", "kết quả tốt");

    // ── Bearish keywords (score -0.5) ─────────────────────────────────
    private static final List<String> BEARISH = List.of(
            "decline", "loss", "fall", "drop", "weak", "miss", "underperform",
            "sell", "negative", "bearish", "concern", "risk", "uncertainty",
            "slowdown", "cut", "layoff", "debt", "deficit", "inflation",
            "recession", "downgrade", "disappointing", "below expectations",
            "struggle", "challenge", "headwind", "reduce", "shrink",
            // Vietnamese
            "giảm", "điều chỉnh", "bán tháo", "áp lực bán", "lo ngại",
            "rủi ro", "suy giảm", "lỗ", "thận trọng", "dưới kỳ vọng");

    // ── Very Bearish keywords (score -1.0) ────────────────────────────
    private static final List<String> VERY_BEARISH = List.of(
            "crash", "collapse", "bankruptcy", "default", "panic", "crisis",
            "plunge", "tumble", "meltdown", "catastrophe", "scandal", "fraud",
            "investigation", "ban", "shutdown", "wipeout", "bloodbath",
            // Vietnamese
            "lao dốc", "phá sản", "vỡ nợ", "khủng hoảng", "xả mạnh",
            "giảm sàn", "sụt giảm mạnh", "thanh tra");

    // ── Tickers: global + Vietnamese HOSE/HNX blue-chips ─────────────
    private static final Pattern TICKER_PATTERN = Pattern.compile(
            "\\$([A-Z]{1,5})"
                    + "|\\b(AAPL|TSLA|NVDA|AMZN|GOOGL|MSFT|META|BTC|ETH|SPY|QQQ)"
                    // Vietnam stocks
                    + "|\\b(VIC|VHM|VNM|FPT|TCB|MBB|VPB|CTG|BID|VCB|ACB|GVR|SAB|MSN|HPG|GAS|POW|PLX|PNJ|MWG|VRE|SSI|VND|HCM|VIB|HDB|DGC|BCM)\\b");

    public record SentimentResult(
            double score,
            String tickers,
            String summary,
            boolean isBullish) implements java.io.Serializable {
        public static SentimentResult neutral() {
            return new SentimentResult(0.0, "", "", false);
        }

        public boolean isBullish(double threshold) {
            return score >= threshold;
        }
    }

    /**
     * Analyzes the title and description using financial keyword matching.
     * Result is cached by title to avoid reprocessing the same article.
     */
    @Cacheable(value = "sentiment", key = "#title.hashCode()")
    public SentimentResult analyze(String title, String description) {
        if (title == null)
            return SentimentResult.neutral();

        String text = (title + " " + (description != null ? description : "")).toLowerCase();
        double score = 0.0;
        int matches = 0;

        for (String kw : VERY_BULLISH) {
            if (text.contains(kw)) {
                score += 1.0;
                matches++;
            }
        }
        for (String kw : BULLISH) {
            if (text.contains(kw)) {
                score += 0.5;
                matches++;
            }
        }
        for (String kw : BEARISH) {
            if (text.contains(kw)) {
                score -= 0.5;
                matches++;
            }
        }
        for (String kw : VERY_BEARISH) {
            if (text.contains(kw)) {
                score -= 1.0;
                matches++;
            }
        }

        // Normalize to [-1, +1] range
        double normalized = matches > 0 ? Math.max(-1.0, Math.min(1.0, score / Math.max(matches, 2))) : 0.0;

        // Extract tickers from original title (uppercase-sensitive)
        String tickers = extractTickers(title + " " + (description != null ? description : ""));

        // Build short summary
        String summary = buildSummary(normalized, tickers);

        log.debug("[Sentiment] title='{}' score={} tickers='{}'", title, normalized, tickers);
        return new SentimentResult(normalized, tickers, summary, normalized >= 0.5);
    }

    private String extractTickers(String text) {
        Matcher m = TICKER_PATTERN.matcher(text);
        Set<String> found = new LinkedHashSet<>();
        while (m.find()) {
            String t = m.group(1) != null ? m.group(1) : m.group(2);
            if (t != null)
                found.add(t);
        }
        return String.join(",", found);
    }

    private String buildSummary(double score, String tickers) {
        String sentiment = score >= 0.7 ? "📈 Bullish"
                : score >= 0.3 ? "🟢 Slightly bullish"
                        : score <= -0.7 ? "📉 Bearish"
                                : score <= -0.3 ? "🔴 Slightly bearish"
                                        : "➡️ Neutral";
        return tickers.isBlank()
                ? sentiment + " signal detected"
                : sentiment + " on " + tickers;
    }
}
