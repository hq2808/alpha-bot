package com.alphabot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "Summary of a portfolio's financial status")
public class PortfolioSummaryResponse implements java.io.Serializable {
    @Schema(description = "Name of the portfolio", example = "AI Auto Trader")
    private String name;

    @Schema(description = "Initial capital at the start", example = "100000000")
    private BigDecimal initialCapital;

    @Schema(description = "Current available cash balance", example = "50000000")
    private BigDecimal cashBalance;

    @Schema(description = "Total equity (Cash + Stock Value)", example = "105000000")
    private BigDecimal totalEquity;

    @Schema(description = "Realized/Unrealized Profit/Loss value", example = "5000000")
    private BigDecimal pnlValue;

    @Schema(description = "Profit/Loss percentage relative to initial capital", example = "5.0")
    private BigDecimal pnlPercent;
}
