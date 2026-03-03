package com.alphabot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "Enriched stock position with current market data and PnL")
public class EnrichedPositionResponse {
    @Schema(description = "Position ID", example = "1")
    private Long id;

    @Schema(description = "Stock ticker symbol", example = "FPT")
    private String ticker;

    @Schema(description = "Quantity owned", example = "100")
    private Integer quantity;

    @Schema(description = "Average purchase price", example = "90000")
    private BigDecimal averagePrice;

    @Schema(description = "Current market price", example = "95000")
    private BigDecimal currentPrice;

    @Schema(description = "Unrealized Profit/Loss value for this position", example = "500000")
    private BigDecimal pnlValue;

    @Schema(description = "Unrealized Profit/Loss percentage", example = "5.56")
    private BigDecimal pnlPercent;
}
