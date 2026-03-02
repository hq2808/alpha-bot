package com.alphabot.dto;

import lombok.Data;

@Data
public class AiRecommendation {
    private String action; // BUY, SELL, HOLD
    private String ticker;
    private double confidence; // 0.0 to 1.0
    private String reason;
}
