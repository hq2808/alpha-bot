package com.alphabot.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SentimentCategory {

    BULLISH(0.5, "📈 Bullish"),
    BEARISH(-0.5, "📉 Bearish"),
    NEUTRAL(0.0, "➡️ Neutral");

    private final double threshold;
    private final String label;

    /**
     * Derives the category from a normalized score [-1, +1].
     * Single source of truth — no magic numbers scattered around.
     */
    public static SentimentCategory fromScore(double score) {
        if (score >= BULLISH.threshold)
            return BULLISH;
        if (score <= BEARISH.threshold)
            return BEARISH;
        return NEUTRAL;
    }
}
