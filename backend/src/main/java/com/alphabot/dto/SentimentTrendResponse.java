package com.alphabot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Daily sentiment trend data point")
public class SentimentTrendResponse {
    @Schema(description = "Date of the trend point", example = "2024-03-01")
    private LocalDate date;

    @Schema(description = "Average sentiment score for the day", example = "0.75")
    private Double avgSentiment;

    @Schema(description = "Total number of articles on this day", example = "42")
    private Long articleCount;
}
