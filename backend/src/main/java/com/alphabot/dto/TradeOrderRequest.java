package com.alphabot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request object for executing a manual trade order")
public class TradeOrderRequest {
    @Schema(description = "Stock ticker symbol", example = "FPT")
    private String ticker;

    @Schema(description = "Quantity of shares", example = "100")
    private int quantity;

    @Schema(description = "Order action (BUY or SELL)", example = "BUY")
    private String action; // BUY, SELL

    @Schema(description = "Reason or note for the trade", example = "Bullish momentum on FPT")
    private String reason;
}
