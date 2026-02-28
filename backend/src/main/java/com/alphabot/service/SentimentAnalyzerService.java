package com.alphabot.service;

import com.alphabot.entity.SentimentCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;

/**
 * Rule-based financial sentiment analyzer using keyword dictionaries.
 * No external API required — works 100% offline.
 *
 * Keywords are matched with regex word boundaries (\b) to prevent false
 * positives,
 * e.g. "gain" must NOT match inside "regain" or "captain".
 *
 * Scores (normalized to [-1, +1]):
 * +1.0 = very bullish ("record high", "kỷ lục")
 * +0.5 = bullish ("growth", "tăng trưởng")
 * 0.0 = neutral
 * -0.5 = bearish ("decline", "giảm")
 * -1.0 = very bearish ("crash", "khủng hoảng")
 */
@Service
@Slf4j
public class SentimentAnalyzerService {

    // Thresholds live in SentimentCategory.threshold — no separate class needed.

    // ── Keyword Enum (weight + words + compiled Pattern) ────────────────────────

    /**
     * Each enum value groups a set of keywords with a shared sentiment weight.
     * Patterns are compiled once at class-load time using word boundaries.
     * 
     * NOTE: Vietnamese words don't have ASCII \b boundaries so we use
     * (?<![\\p{L}]) / (?![\\p{L}]) (Unicode-aware boundary) for them.
     */
    public enum SentimentKeyword {

        VERY_BULLISH(1.0, // English
                "record high", "all-time high", "beat expectations", "blowout earnings",
                "massive rally", "skyrocket", "surge", "soar", "breakout",
                "historic gain", "bull run", "explosive growth", // Vietnamese
                "kỷ lục", "bứt phá", "tăng mạnh nhất", "đỉnh lịch sử",
                "vượt kỳ vọng", "tăng trần", "tăng mạnh"),

        BULLISH(0.5, // English
                "growth", "profit", "gain", "rise", "rally", "upturn", "rebound",
                "recovery", "upgrade", "beat", "outperform", "positive",
                "strong", "expand", "increase", "boost", "opportunity",
                "optimism", "confidence", "investment", "revenue",
                "partnership", "deal", "acquisition", "innovation", "exceeded", // Vietnamese
                "lợi nhuận", "tăng trưởng", "khởi sắc", "phục hồi",
                "tích cực", "mua vào", "dòng tiền vào", "vượt kế hoạch",
                "cổ tức cao", "ngoại khối mua ròng", "kết quả tốt"),

        BEARISH(-0.5, // English
                "decline", "loss", "fall", "drop", "weak", "miss", "underperform",
                "negative", "concern", "risk", "uncertainty",
                "slowdown", "layoff", "debt", "deficit", "inflation",
                "recession", "downgrade", "disappointing", "below expectations",
                "struggle", "challenge", "headwind", "reduce", "shrink", // Vietnamese
                "điều chỉnh", "bán tháo", "áp lực bán", "lo ngại",
                "rủi ro", "suy giảm", "thận trọng", "dưới kỳ vọng"),

        VERY_BEARISH(-1.0, // English
                "crash", "collapse", "bankruptcy", "default", "panic", "crisis",
                "plunge", "tumble", "meltdown", "catastrophe", "scandal", "fraud",
                "investigation", "shutdown", "wipeout", "bloodbath", // Vietnamese
                "lao dốc", "phá sản", "vỡ nợ", "khủng hoảng", "xả mạnh",
                "giảm sàn", "sụt giảm mạnh", "thanh tra");

        /** Sentiment weight for this keyword group. */
        public final double weight;

        /** Precompiled pattern — all keywords joined with Unicode-aware boundaries. */
        private final Pattern pattern;

        SentimentKeyword(double weight, String... keywords) {
            this.weight = weight;
            // Build alternation: keyword1|keyword2|...
            // Use (?i) for case-insensitive and Unicode-aware word boundaries
            String alternation = Arrays.stream(keywords)
                    .map(kw -> Pattern.quote(kw))
                    .collect(java.util.stream.Collectors.joining("|"));
            // (?<!\p{L}) = not preceded by a Unicode letter (safe for Vietnamese too)
            // (?!\p{L}) = not followed by a Unicode letter
            this.pattern = Pattern.compile(
                    "(?i)(?<!\\p{L})(?:" + alternation + ")(?!\\p{L})");
        }

        /** Returns true if the text contains at least one keyword from this group. */
        public boolean matches(String text) {
            return pattern.matcher(text).find();
        }

        /** Returns the count of distinct keyword matches in the text. */
        public int countMatches(String text) {
            Matcher m = pattern.matcher(text);
            int count = 0;
            while (m.find())
                count++;
            return count;
        }
    }

    // ── Tickers: global + Vietnamese HOSE/HNX blue-chips ─────────────────────

    private static final Pattern TICKER_PATTERN = Pattern.compile(
            "\\$([A-Z]{1,5})"
                    + "|\\b(AAPL|TSLA|NVDA|AMZN|GOOGL|MSFT|META|BTC|ETH|VND|USD|XAU|XAG)\\b"
                    // Vietnam VN30 & Popular tickers
                    + "|\\b(VIC|VHM|VNM|FPT|TCB|MBB|VPB|CTG|BID|VCB|ACB|GVR|SAB|MSN|HPG|GAS|POW|PLX|PNJ|MWG|VRE|SSI|VND|HCM|VIB|HDB|DGC|BCM|STB|NVL|KDH|PDR|VJC|VSH|DXG|DIG|NLG|KBC|VCI|LPB|MSB|SHB|OCB|EIB|TPB|TCH|KOS|VGC|PVD|PVS|VCS|DHT|VIX|GEX|REE|VHC|ANV|TNG|SCS|CTR|VTP|ACV|BSR|OIL|GEG|HDG|VPI|SZC|IDC|DPR|PHR|NTC|AGG|CRE|SCR|DIG|PDR|HTN|QCG|HQC|ITA|FLC|ROS|HAI|ART)\\b");

    // ── SentimentResult ───────────────────────────────────────────────────────

    public record SentimentResult(
            double score,
            String tickers,
            String summary,
            String tags,
            SentimentCategory category) implements java.io.Serializable {

        public static SentimentResult neutral() {
            return new SentimentResult(0.0, "", "", "", SentimentCategory.NEUTRAL);
        }

        public boolean isBullish(double threshold) {
            return score >= threshold && category == SentimentCategory.BULLISH;
        }
    }

    // ── analyze ───────────────────────────────────────────────────────────────

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

        // One loop over the enum — no more 4 separate lists
        for (SentimentKeyword group : SentimentKeyword.values()) {
            int count = group.countMatches(text);
            if (count > 0) {
                score += group.weight * count;
            }
        }

        // Normalize to (-1, +1) using tanh — smooth, no hard clipping needed.
        // tanh(0) = 0, tanh(1) ≈ 0.76, tanh(2) ≈ 0.96, tanh(∞) → 1
        double normalized = Math.tanh(score);

        String tickers = extractTickers(title + " " + (description != null ? description : ""));
        String summary = buildSummary(normalized, tickers);
        String tags = extractTags(text);

        SentimentCategory category = SentimentCategory.fromScore(normalized);

        log.debug("[Sentiment] title='{}' score={} tickers='{}' tags='{}' category={}",
                title, normalized, tickers, tags, category);
        return new SentimentResult(normalized, tickers, summary, tags, category);
    }

    // ── Tag extraction ────────────────────────────────────────────────────────

    private static final Pattern TAG_MACRO = Pattern
            .compile("(?i)lãi suất|lạm phát|\\bgdp\\b|tỷ giá|vĩ mô|interest rate|inflation|monetary policy");
    private static final Pattern TAG_BANKING = Pattern
            .compile("(?i)ngân hàng|tín dụng|nợ xấu|\\bnnhh\\b|\\bfed\\b|banking|credit|bad debt");
    private static final Pattern TAG_REALESTATE = Pattern
            .compile("(?i)bất động sản|đất nền|chung cư|sổ đỏ|real estate|property|housing");
    private static final Pattern TAG_DIVIDEND = Pattern.compile("(?i)cổ tức|phát hành|phân bổ|dividend|share issuance");
    private static final Pattern TAG_POLICY = Pattern
            .compile("(?i)chính sách|quốc hội|chính phủ|thủ tướng|\\bluật\\b|policy|regulation|government");

    private String extractTags(String text) {
        Set<String> tags = new LinkedHashSet<>();
        if (TAG_MACRO.matcher(text).find())
            tags.add("[Vĩ Mô]");
        if (TAG_BANKING.matcher(text).find())
            tags.add("[Ngân Hàng]");
        if (TAG_REALESTATE.matcher(text).find())
            tags.add("[Bất Động Sản]");
        if (TAG_DIVIDEND.matcher(text).find())
            tags.add("[Cổ Tức]");
        if (TAG_POLICY.matcher(text).find())
            tags.add("[Chính Sách]");
        if (tags.isEmpty())
            tags.add("[Thị Trường]");
        return String.join(" ", tags);
    }

    // ── Ticker extraction ─────────────────────────────────────────────────────

    private String extractTickers(String text) {
        Matcher m = TICKER_PATTERN.matcher(text);
        Set<String> found = new LinkedHashSet<>();
        while (m.find()) {
            String t = (m.group(1) != null) ? m.group(1)
                    : (m.group(2) != null) ? m.group(2)
                            : m.group(3);
            if (t != null)
                found.add(t.toUpperCase());
        }
        return String.join(",", found);
    }

    // ── Summary builder ───────────────────────────────────────────────────────

    private String buildSummary(double score, String tickers) {
        SentimentCategory cat = SentimentCategory.fromScore(score);
        String sentiment = switch (cat) {
            case BULLISH -> score >= 0.7 ? cat.getLabel() : "🟢 Slightly bullish";
            case BEARISH -> score <= -0.7 ? cat.getLabel() : "🔴 Slightly bearish";
            case NEUTRAL -> "➡️ Neutral";
        };
        return tickers.isBlank() ? sentiment + " signal detected" : sentiment + " on " + tickers;
    }
}
