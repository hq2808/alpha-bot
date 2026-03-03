package com.alphabot.dto;

import lombok.Data;

@Data
public class TradeOrderRequest {
    private String ticker;
    private int quantity;
    private String action; // BUY, SELL
    private String reason;
}
